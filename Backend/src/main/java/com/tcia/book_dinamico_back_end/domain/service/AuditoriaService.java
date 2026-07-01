package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.domain.model.Auditoria;
import com.tcia.book_dinamico_back_end.domain.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public void salvar(Auditoria auditoria) {
        repository.save(auditoria);
    }
}
