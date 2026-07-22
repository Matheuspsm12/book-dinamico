package com.tcia.book_dinamico_back_end.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "documento_aba")
public class DocumentoAba implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documento_aba_id_seq")
    @SequenceGenerator(name = "documento_aba_id_seq", sequenceName = "documento_aba_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "nome_aba", nullable = false, length = 255)
    private String nomeAba;

    @Column(name = "qtd_linhas", nullable = false)
    private Integer qtdLinhas;

    @Column(name = "qtd_colunas", nullable = false)
    private Integer qtdColunas;
}
