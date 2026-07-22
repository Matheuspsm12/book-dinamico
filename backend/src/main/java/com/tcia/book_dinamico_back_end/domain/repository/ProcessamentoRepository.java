package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.Processamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessamentoRepository extends JpaRepository<Processamento, Long> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "documento"})
    Page<Processamento> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"usuario", "documento"})
    Optional<Processamento> findById(Long id);

    @EntityGraph(attributePaths = {"usuario", "documento"})
    Page<Processamento> findByTipoProcessamento(Integer tipoProcessamento, Pageable pageable);

    List<Processamento> findByExecutadoFalseOrReprocessarTrue();

    @Modifying
    @Query("delete from Processamento p where p.documento.id = :documentoId")
    void deleteByDocumentoId(@Param("documentoId") Long documentoId);
}
