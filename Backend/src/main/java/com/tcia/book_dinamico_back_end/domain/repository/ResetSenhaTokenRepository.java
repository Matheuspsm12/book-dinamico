package com.tcia.book_dinamico_back_end.domain.repository;

import com.tcia.book_dinamico_back_end.domain.model.ResetSenhaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResetSenhaTokenRepository extends JpaRepository<ResetSenhaToken, Long> {

    Optional<ResetSenhaToken> findByToken(String token);
}
