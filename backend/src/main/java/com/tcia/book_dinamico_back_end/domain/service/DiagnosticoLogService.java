package com.tcia.book_dinamico_back_end.domain.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.tcia.book_dinamico_back_end.api.response.LogConsultaResponse;
import com.tcia.book_dinamico_back_end.api.response.LogLinhaResponse;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import lombok.extern.log4j.Log4j2;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lê as últimas linhas do arquivo de log da aplicação para a tela de Diagnóstico
 * (somente ADMIN). Não depende de configuração extra: descobre o arquivo ativo
 * inspecionando os appenders do Logback em tempo de execução, então funciona em
 * qualquer perfil que tenha um {@code FileAppender} (dev/hom/prod).
 */
@Log4j2
@Service
public class DiagnosticoLogService {

    /** Casa o pattern dos appenders de arquivo: {@code yyyy-MM-dd HH:mm:ss LEVEL logger - msg}. */
    private static final Pattern LINHA_LOG = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?)\\s+"
                    + "(TRACE|DEBUG|INFO|WARN|ERROR)\\s+(\\S+)\\s+-\\s?(.*)$");

    private static final List<String> ORDEM_NIVEIS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    private static final int LINHAS_MIN = 10;
    private static final int LINHAS_MAX = 2_000;
    /** Com filtro ativo, lê mais linhas cruas para ter material suficiente antes de recortar. */
    private static final int LINHAS_CRUAS_MAX = 20_000;
    /** Teto de bytes lidos a partir do fim do arquivo — evita OOM com linhas anômalas. */
    private static final long TETO_LEITURA_BYTES = 12L * 1024 * 1024;
    private static final Pattern NOME_ARQUIVO_LOG = Pattern.compile("[A-Za-z0-9._-]+\\.log");

    /** Fallback opcional; normalmente vazio, pois o caminho vem do próprio Logback. */
    @Value("${app.diagnostico.caminho-log:}")
    private String caminhoLogConfigurado;

    public LogConsultaResponse consultar(int linhas, String nivel, String busca, String arquivo) {
        int limite = Math.max(LINHAS_MIN, Math.min(LINHAS_MAX, linhas));
        String nivelMin = normalizarNivel(nivel);
        String termo = (busca != null && !busca.isBlank()) ? busca.trim() : null;

        Path arquivoAtivo = localizarArquivoLog();
        if (arquivoAtivo == null) {
            return LogConsultaResponse.builder()
                    .logEmArquivoAtivo(false)
                    .mensagemInativo("O log em arquivo não está ativo neste ambiente. A aplicação "
                            + "grava em disco apenas nos perfis com appender de arquivo (dev/hom/prod).")
                    .arquivosDisponiveis(List.of())
                    .geradoEm(LocalDateTime.now())
                    .totalLinhas(0)
                    .linhas(List.of())
                    .build();
        }

        Path diretorio = arquivoAtivo.getParent();
        List<String> disponiveis = listarArquivos(diretorio, arquivoAtivo);

        Path alvo = arquivoAtivo;
        if (arquivo != null && !arquivo.isBlank()
                && !arquivo.equals(arquivoAtivo.getFileName().toString())) {
            alvo = resolverArquivoSolicitado(diretorio, arquivo);
        }

        boolean comFiltro = nivelMin != null || termo != null;
        int linhasCruas = comFiltro
                ? Math.min(LINHAS_CRUAS_MAX, Math.max(limite * 6, 3_000))
                : limite;

        List<LogLinhaResponse> entradas = parsear(tail(alvo, linhasCruas));
        List<LogLinhaResponse> filtradas = filtrar(entradas, nivelMin, termo);
        if (filtradas.size() > limite) {
            filtradas = new ArrayList<>(filtradas.subList(filtradas.size() - limite, filtradas.size()));
        }

        return LogConsultaResponse.builder()
                .logEmArquivoAtivo(true)
                .arquivo(alvo.getFileName().toString())
                .arquivosDisponiveis(disponiveis)
                .geradoEm(LocalDateTime.now())
                .totalLinhas(filtradas.size())
                .linhas(filtradas)
                .build();
    }

    // ----------------------------------------------------------------- descoberta do arquivo

    private Path localizarArquivoLog() {
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext ctx) {
            Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
            for (Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders(); it.hasNext(); ) {
                Appender<ILoggingEvent> ap = it.next();
                if (ap instanceof FileAppender<?> fileAppender && fileAppender.getFile() != null) {
                    return Path.of(fileAppender.getFile()).toAbsolutePath().normalize();
                }
            }
        }
        if (caminhoLogConfigurado != null && !caminhoLogConfigurado.isBlank()) {
            return Path.of(caminhoLogConfigurado).toAbsolutePath().normalize();
        }
        return null;
    }

    private List<String> listarArquivos(Path diretorio, Path ativo) {
        String nomeAtivo = ativo.getFileName().toString();
        if (diretorio == null || !Files.isDirectory(diretorio)) {
            return List.of(nomeAtivo);
        }
        try (Stream<Path> arquivos = Files.list(diretorio)) {
            List<String> nomes = arquivos
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".log"))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toCollection(ArrayList::new));
            nomes.remove(nomeAtivo);
            nomes.add(0, nomeAtivo);
            return nomes;
        } catch (IOException e) {
            log.warn("Falha ao listar arquivos de log em {}: {}", diretorio, e.getMessage());
            return List.of(nomeAtivo);
        }
    }

    private Path resolverArquivoSolicitado(Path diretorio, String nome) {
        if (!NOME_ARQUIVO_LOG.matcher(nome).matches()) {
            throw new NegocioException("diagnostico-arquivo-invalido");
        }
        Path alvo = diretorio.resolve(nome).normalize();
        if (!diretorio.equals(alvo.getParent()) || !Files.isRegularFile(alvo)) {
            throw new NegocioException("diagnostico-arquivo-nao-encontrado");
        }
        return alvo;
    }

    // ----------------------------------------------------------------- leitura / parsing

    /** Lê as últimas {@code maxLinhas} linhas do arquivo sem carregar o todo. */
    private List<String> tail(Path arquivo, int maxLinhas) {
        if (!Files.isRegularFile(arquivo)) {
            return List.of();
        }
        try (RandomAccessFile raf = new RandomAccessFile(arquivo.toFile(), "r")) {
            long tamanho = raf.length();
            if (tamanho == 0) {
                return List.of();
            }
            long limiteInferior = Math.max(0, tamanho - TETO_LEITURA_BYTES);
            long inicio = limiteInferior;
            long pos = tamanho;
            int novasLinhas = 0;
            byte[] buffer = new byte[8_192];

            while (pos > limiteInferior) {
                int chunk = (int) Math.min(buffer.length, pos - limiteInferior);
                pos -= chunk;
                raf.seek(pos);
                raf.readFully(buffer, 0, chunk);
                for (int i = chunk - 1; i >= 0; i--) {
                    if (buffer[i] == '\n' && ++novasLinhas > maxLinhas) {
                        inicio = pos + i + 1;
                        pos = limiteInferior;
                        break;
                    }
                }
            }

            raf.seek(inicio);
            byte[] resto = new byte[(int) (tamanho - inicio)];
            raf.readFully(resto);
            return new String(resto, StandardCharsets.UTF_8).lines()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            log.error("Falha ao ler arquivo de log {}: {}", arquivo, e.getMessage());
            throw new NegocioException("diagnostico-falha-leitura");
        }
    }

    List<LogLinhaResponse> parsear(List<String> linhas) {
        List<LogLinhaResponse> resultado = new ArrayList<>();
        LogLinhaResponse ultima = null;
        StringBuilder stack = null;

        for (String linha : linhas) {
            Matcher m = LINHA_LOG.matcher(linha);
            if (m.matches()) {
                anexarStack(ultima, stack);
                stack = null;
                ultima = LogLinhaResponse.builder()
                        .timestamp(m.group(1))
                        .nivel(m.group(2))
                        .logger(m.group(3))
                        .mensagem(m.group(4))
                        .build();
                resultado.add(ultima);
            } else if (linha.isBlank()) {
                // separadores em branco não interessam
            } else if (ultima != null) {
                if (stack == null) {
                    stack = new StringBuilder(linha);
                } else {
                    stack.append('\n').append(linha);
                }
            } else {
                ultima = LogLinhaResponse.builder().nivel("RAW").mensagem(linha).build();
                resultado.add(ultima);
            }
        }
        anexarStack(ultima, stack);
        return resultado;
    }

    private void anexarStack(LogLinhaResponse entrada, StringBuilder stack) {
        if (entrada != null && stack != null) {
            entrada.setStacktrace(stack.toString());
        }
    }

    private List<LogLinhaResponse> filtrar(List<LogLinhaResponse> entradas, String nivelMin, String termo) {
        int minRank = nivelMin == null ? -1 : ORDEM_NIVEIS.indexOf(nivelMin);
        String termoLower = termo == null ? null : termo.toLowerCase(Locale.ROOT);

        List<LogLinhaResponse> saida = new ArrayList<>();
        for (LogLinhaResponse e : entradas) {
            if (minRank >= 0 && ORDEM_NIVEIS.indexOf(e.getNivel()) < minRank) {
                continue; // RAW (rank -1) sai quando se filtra por nível
            }
            if (termoLower != null && !contem(e, termoLower)) {
                continue;
            }
            saida.add(e);
        }
        return saida;
    }

    private boolean contem(LogLinhaResponse e, String termoLower) {
        return textoContem(e.getMensagem(), termoLower)
                || textoContem(e.getLogger(), termoLower)
                || textoContem(e.getStacktrace(), termoLower);
    }

    private boolean textoContem(String valor, String termoLower) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termoLower);
    }

    private String normalizarNivel(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            return null;
        }
        String up = nivel.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(up) || "ALL".equals(up)) {
            return null;
        }
        return ORDEM_NIVEIS.contains(up) ? up : null;
    }
}
