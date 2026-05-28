package com.tcia.book_dinamico_back_end.repository;

import com.tcia.book_dinamico_back_end.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    List<Documento> findByAtivoTrueOrderByAtualizadoEmDesc();
}
