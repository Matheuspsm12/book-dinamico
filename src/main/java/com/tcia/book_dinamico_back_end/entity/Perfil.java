package com.tcia.book_dinamico_back_end.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Perfil de acesso (base padrão TCIA — definida pelo Efren). No Book Dinâmico os perfis
 * são ADMIN e USUARIO, substituindo o antigo enum {@code role}.
 *
 * @author TCIA
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "perfil")
public class Perfil implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "perfil_seq")
    @SequenceGenerator(name = "perfil_seq", sequenceName = "perfil_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nome_perfil", nullable = false, unique = true, length = 255)
    private String nomePerfil;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "ativado", nullable = false)
    private Boolean ativado;

    @JsonManagedReference
    @OneToMany(mappedBy = "perfil", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PerfilPermissao> perfilPermissao;

    @PrePersist
    private void prePersist() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (ativado == null) ativado = true;
    }
}
