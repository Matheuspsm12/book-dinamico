package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByStatus(UsuarioStatus status);

    java.util.List<Usuario> findByStatus(UsuarioStatus status);

    @Modifying
    @Query("update Usuario u set u.ultimoAcesso = :agora, u.ociosidadeNotificadoEm = null where u.id = :id")
    void registrarAcesso(@Param("id") Long id, @Param("agora") LocalDateTime agora);
}
