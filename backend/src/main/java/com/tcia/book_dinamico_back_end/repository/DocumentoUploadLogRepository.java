package com.tcia.book_dinamico_back_end.repository;

import com.tcia.book_dinamico_back_end.entity.DocumentoUploadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoUploadLogRepository extends JpaRepository<DocumentoUploadLog, Long> {

    List<DocumentoUploadLog> findByDocumentoIdOrderByDatetimeDesc(Long documentoId);
}
