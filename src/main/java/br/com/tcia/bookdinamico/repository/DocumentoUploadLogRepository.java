package br.com.tcia.bookdinamico.repository;

import br.com.tcia.bookdinamico.entity.DocumentoUploadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoUploadLogRepository extends JpaRepository<DocumentoUploadLog, Long> {

    List<DocumentoUploadLog> findByDocumentoIdOrderByDatetimeDesc(Long documentoId);
}
