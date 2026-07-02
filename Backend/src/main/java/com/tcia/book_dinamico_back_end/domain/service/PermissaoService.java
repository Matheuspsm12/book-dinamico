package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.api.request.PermissaoRequest;
import com.tcia.book_dinamico_back_end.api.response.PermissaoResponse;
import com.tcia.book_dinamico_back_end.core.annotation.Auditar;
import com.tcia.book_dinamico_back_end.core.enums.AuditoriaAcaoEnum;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Permissao;
import com.tcia.book_dinamico_back_end.domain.repository.PermissaoRepository;
import com.tcia.book_dinamico_back_end.infrastructure.mapper.PermissaoMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissaoService {

    private final PermissaoRepository repository;
    private final PermissaoMapper mapper;

    public List<PermissaoResponse> listar() {
        return mapper.toResponseList(repository.findAll());
    }

    public Permissao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NegocioException("Permissão não encontrada."));
    }

    @Transactional
    @Auditar(mensagem = "Permissão criada", acao = AuditoriaAcaoEnum.CRIAR_PERMISSAO)
    public PermissaoResponse criar(PermissaoRequest request) {
        validarNomeRepetido(request.getNomePermissao());

        Permissao permissao = Permissao.builder()
                .nomePermissao(request.getNomePermissao())
                .descricao(request.getDescricao())
                .build();

        return mapper.toResponse(repository.save(permissao));
    }

    @Transactional
    @Auditar(mensagem = "Permissão alterada", acao = AuditoriaAcaoEnum.ALTERAR_PERMISSAO)
    public void atualizar(Long id, PermissaoRequest request) {
        Permissao permissao = buscarPorId(id);
        permissao.setNomePermissao(request.getNomePermissao());
        permissao.setDescricao(request.getDescricao());
        repository.save(permissao);
    }

    @Transactional
    @Auditar(mensagem = "Permissão excluída", acao = AuditoriaAcaoEnum.EXCLUIR_PERMISSAO)
    public void deletar(Long id) {
        Permissao permissao = buscarPorId(id);
        repository.delete(permissao);
    }

    private void validarNomeRepetido(String nomePermissao) {
        if (repository.existsByNomePermissaoIgnoreCase(nomePermissao)) {
            throw new NegocioException("Já existe uma permissão para o nome informado.");
        }
    }
}
