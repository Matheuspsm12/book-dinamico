package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.DocumentoAba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoAbaRepository extends JpaRepository<DocumentoAba, Long> {

    void deleteByDocumentoId(Long documentoId);
}
