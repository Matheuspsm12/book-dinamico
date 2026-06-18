package com.tcia.book_dinamico_back_end.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Relação usuário ↔ perfil (base padrão TCIA — definida pelo Efren).
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
@Table(name = "usuario_perfil")
public class UsuarioPerfil implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_perfil_seq")
    @SequenceGenerator(name = "usuario_perfil_seq", sequenceName = "usuario_perfil_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Perfil perfil;
}
