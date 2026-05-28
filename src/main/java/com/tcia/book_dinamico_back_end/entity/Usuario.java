package com.tcia.book_dinamico_back_end.entity;

import com.tcia.book_dinamico_back_end.enums.UsuarioRole;
import com.tcia.book_dinamico_back_end.enums.UsuarioStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuario")
@EntityListeners(AuditingEntityListener.class)
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_id_seq")
    @SequenceGenerator(name = "usuario_id_seq", sequenceName = "usuario_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nome", nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String nome;

    @Column(name = "empresa", nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String empresa;

    @Column(name = "email", nullable = false, length = 200, unique = true)
    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    @NotBlank
    private String senhaHash;

    @Column(name = "justificativa", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    private UsuarioStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @NotNull
    private UsuarioRole role;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private Usuario aprovadoPor;

    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    /**
     * Authorities exposed to Spring Security. Mapeia {@link UsuarioRole} para o formato
     * esperado pelo {@code hasRole(...)} (prefixo {@code ROLE_}).
     */
    @Transient
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
