package com.tcia.book_dinamico_back_end.domain.service;

import com.tcia.book_dinamico_back_end.domain.exception.NegocioException;
import com.tcia.book_dinamico_back_end.domain.model.Dominio;
import com.tcia.book_dinamico_back_end.domain.repository.DominioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DominioService {

    private final DominioRepository repository;

    @Transactional(readOnly = true)
    public Dominio buscarPorChave(String chave) {
        return repository.findById(chave).orElseThrow(() -> new NegocioException("Chave não encontrada"));
    }
}
