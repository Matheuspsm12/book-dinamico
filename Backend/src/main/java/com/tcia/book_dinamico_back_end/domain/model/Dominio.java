package com.tcia.book_dinamico_back_end.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "dominio")
public class Dominio {
    @Id
    @Column(name = "dominio_chave", length = 60)
    private String chave;

    @Column(name = "dominio_valor", length = 370)
    private String valor;

    @Column(name = "dominio_descricao")
    private String descricao;
}
