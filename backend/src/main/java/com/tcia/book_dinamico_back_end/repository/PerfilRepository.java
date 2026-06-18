package com.tcia.book_dinamico_back_end.repository;

import com.tcia.book_dinamico_back_end.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByNomePerfil(String nomePerfil);
}
