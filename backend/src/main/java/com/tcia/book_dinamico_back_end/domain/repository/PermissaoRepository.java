package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

    Optional<Permissao> findByNomePermissao(String nomePermissao);

    boolean existsByNomePermissaoIgnoreCase(String nomePermissao);
}
