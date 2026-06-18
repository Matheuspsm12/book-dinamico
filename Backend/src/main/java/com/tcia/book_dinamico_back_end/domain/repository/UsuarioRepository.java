package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByStatus(UsuarioStatus status);
}
