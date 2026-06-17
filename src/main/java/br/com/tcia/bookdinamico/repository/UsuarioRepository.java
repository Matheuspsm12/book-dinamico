package br.com.tcia.bookdinamico.repository;

import br.com.tcia.bookdinamico.entity.Usuario;
import br.com.tcia.bookdinamico.enums.UsuarioStatus;
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
