package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessamentoRepository extends JpaRepository<Processamento, Long> {

    Page<Processamento> findByTipoProcessamento(Integer tipoProcessamento, Pageable pageable);
}
