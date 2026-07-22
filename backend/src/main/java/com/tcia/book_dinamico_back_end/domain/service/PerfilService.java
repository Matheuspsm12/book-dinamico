package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.api.request.PerfilRequest;
import com.tcia.book_dinamico_back_end.api.response.PerfilResponse;
import com.tcia.book_dinamico_back_end.core.annotation.Auditar;
import com.tcia.book_dinamico_back_end.core.enums.AuditoriaAcaoEnum;
import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Perfil;
import com.tcia.book_dinamico_back_end.domain.model.PerfilPermissao;
import com.tcia.book_dinamico_back_end.domain.model.Permissao;
import com.tcia.book_dinamico_back_end.domain.repository.PerfilRepository;
import com.tcia.book_dinamico_back_end.infrastructure.mapper.PerfilMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository repository;
    private final PerfilMapper mapper;
    private final PermissaoService permissaoService;

    public List<PerfilResponse> listar() {
        return mapper.toResponseList(repository.findAll());
    }

    public PerfilResponse buscar(Long id) {
        return mapper.toResponse(buscarPorId(id));
    }

    public Perfil buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NegocioException("Perfil não encontrado."));
    }

    @Transactional
    @Auditar(mensagem = "Perfil criado", acao = AuditoriaAcaoEnum.CRIAR_PERFIL)
    public PerfilResponse criar(PerfilRequest request) {
        validarNomeRepetido(request.getNomePerfil());

        Perfil perfil = new Perfil();
        perfil.setNomePerfil(request.getNomePerfil());
        perfil.setDescricao(request.getDescricao());
        perfil.setPerfilPermissao(new ArrayList<>());

        request.getPermissoesIds().forEach(permissaoId -> {
            Permissao permissao = permissaoService.buscarPorId(permissaoId);
            PerfilPermissao perfilPermissao = new PerfilPermissao();
            perfilPermissao.setPerfil(perfil);
            perfilPermissao.setPermissao(permissao);
            perfil.getPerfilPermissao().add(perfilPermissao);
        });

        return mapper.toResponse(repository.save(perfil));
    }

    @Transactional
    @Auditar(mensagem = "Perfil alterado", acao = AuditoriaAcaoEnum.ALTERAR_PERFIL)
    public void atualizar(Long id, PerfilRequest request) {
        Perfil perfil = buscarPorId(id);
        perfil.setNomePerfil(request.getNomePerfil());
        perfil.setDescricao(request.getDescricao());

        List<Long> novosIds = request.getPermissoesIds();
        List<PerfilPermissao> atuais = perfil.getPerfilPermissao();

        atuais.removeIf(pp -> !novosIds.contains(pp.getPermissao().getId()));

        novosIds.forEach(permissaoId -> {
            boolean existe = atuais.stream()
                    .anyMatch(pp -> pp.getPermissao().getId().equals(permissaoId));
            if (!existe) {
                Permissao permissao = permissaoService.buscarPorId(permissaoId);
                PerfilPermissao perfilPermissao = new PerfilPermissao();
                perfilPermissao.setPerfil(perfil);
                perfilPermissao.setPermissao(permissao);
                atuais.add(perfilPermissao);
            }
        });

        repository.save(perfil);
    }

    @Transactional
    @Auditar(mensagem = "Perfil excluído", acao = AuditoriaAcaoEnum.EXCLUIR_PERFIL)
    public void deletar(Long id) {
        Perfil perfil = buscarPorId(id);
        perfil.setAtivado(false);
        repository.save(perfil);
    }

    private void validarNomeRepetido(String nomePerfil) {
        if (repository.existsByNomePerfilIgnoreCase(nomePerfil)) {
            throw new NegocioException("Já existe um perfil para o nome informado.");
        }
    }
}
