package com.tcia.book_dinamico_back_end.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "perfil_permissao")
public class PerfilPermissao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "perfil_permissao_seq")
    @SequenceGenerator(name = "perfil_permissao_seq", sequenceName = "perfil_permissao_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permissao_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Permissao permissao;
}
