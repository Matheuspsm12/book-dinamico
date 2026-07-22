package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.Dominio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DominioRepository extends JpaRepository<Dominio, String> {
}
