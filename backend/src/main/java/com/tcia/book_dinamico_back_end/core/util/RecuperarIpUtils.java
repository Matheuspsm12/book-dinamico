package com.tcia.book_dinamico_back_end.core.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * Resolve o IP do cliente atrás de proxy.
 *
 * <p>Somente o header {@code X-Forwarded-For} é confiável: procuramos o header a
 * partir do fim da lista (o valor anexado pelo proxy mais próximo), validando o
 * formato do endereço. Headers não padronizados (ex.: {@code Proxy-Client-IP}) são
 * descartados porque qualquer cliente pode forjá-los.
 *
 * <p><b>Importante:</b> o proxy reverso deve sobrescrever/limpar o
 * {@code X-Forwarded-For} recebido do cliente (ex.: nginx com
 * {@code proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for}).
 */
public final class RecuperarIpUtils {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$");

    private static final Pattern IPV6 = Pattern.compile(
            "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
                    + "|^(?:[0-9a-fA-F]{1,4}:)*::(?:[0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{0,4}$");

    private RecuperarIpUtils() {
    }

    public static String obterIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = extrairDoXForwardedFor(request.getHeader(HEADER_X_FORWARDED_FOR));
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip != null && !ip.isBlank() ? ip : "unknown";
    }

    private static String extrairDoXForwardedFor(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String[] partes = header.split(",");
        for (int i = partes.length - 1; i >= 0; i--) {
            String candidato = partes[i].trim();
            if (ehIpValido(candidato)) {
                return candidato;
            }
        }
        return null;
    }

    private static boolean ehIpValido(String valor) {
        return valor != null && (IPV4.matcher(valor).matches() || IPV6.matcher(valor).matches());
    }
}