package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.api.response.AuditoriaResponse;
import com.tcia.book_dinamico_back_end.core.enums.EntidadeAuditoriaEnum;
import com.tcia.book_dinamico_back_end.domain.model.Auditoria;
import com.tcia.book_dinamico_back_end.domain.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public void salvar(Auditoria auditoria) {
        repository.save(auditoria);
    }

    public Page<AuditoriaResponse> listarHistoricoDocumentos(Pageable pageable) {
        return repository
                .findByEntidadeOrderByDataHoraDesc(EntidadeAuditoriaEnum.DOCUMENTO.name(), pageable)
                .map(this::toResponse);
    }

    public Page<AuditoriaResponse> listarHistoricoUsuarios(Pageable pageable) {
        return repository
                .findByEntidadeOrderByDataHoraDesc(EntidadeAuditoriaEnum.USUARIO.name(), pageable)
                .map(this::toResponse);
    }

    private AuditoriaResponse toResponse(Auditoria a) {
        return AuditoriaResponse.builder()
                .id(a.getId())
                .usuario(a.getUsuario())
                .acao(a.getAcao())
                .entidade(a.getEntidade())
                .entidadeId(a.getEntidadeId())
                .detalhes(a.getDetalhes())
                .dataHora(a.getDataHora())
                .build();
    }
}
