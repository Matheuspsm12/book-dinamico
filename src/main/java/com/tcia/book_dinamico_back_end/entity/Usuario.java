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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade que representa um usuário do Book Dinâmico.
 *
 * <p>O ciclo de vida segue {@link UsuarioStatus} (PENDENTE → APROVADO/REJEITADO → DESATIVADO)
 * e o papel de acesso é definido por {@link UsuarioRole}. A auto-relação {@code aprovadoPor}
 * registra qual admin decidiu o cadastro.</p>
 *
 * @author TCIA
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "usuario")
@EntityListeners(AuditingEntityListener.class)
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do usuário.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_id_seq")
    @SequenceGenerator(name = "usuario_id_seq", sequenceName = "usuario_seq", allocationSize = 1)
    @Column(name = "id", nullable = false, unique = true)
    @ToString.Include
    private Long id;

    /**
     * Nome do usuário.
     */
    @Column(name = "nome", nullable = false, length = 200)
    @NotBlank(message = "{usuario.nome.not-blank}")
    @Size(max = 200, message = "{usuario.nome.size}")
    private String nome;

    /**
     * Empresa à qual o usuário pertence.
     */
    @Column(name = "empresa", nullable = false, length = 200)
    @NotBlank(message = "{usuario.empresa.not-blank}")
    @Size(max = 200, message = "{usuario.empresa.size}")
    private String empresa;

    /**
     * E-mail do usuário, único no sistema.
     */
    @Column(name = "email", nullable = false, length = 200, unique = true)
    @NotBlank(message = "{usuario.email.not-blank}")
    @Email(message = "{usuario.email.invalido}")
    @Size(max = 200, message = "{usuario.email.size}")
    @ToString.Include
    private String email;

    /**
     * Hash BCrypt da senha do usuário.
     */
    @Column(name = "senha_hash", nullable = false, length = 255)
    @NotBlank(message = "{usuario.senha.not-blank}")
    private String senhaHash;

    /**
     * Justificativa informada no cadastro.
     */
    @Column(name = "justificativa", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "{usuario.justificativa.not-blank}")
    private String justificativa;

    /**
     * Estado atual do cadastro do usuário.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "{usuario.status.not-null}")
    private UsuarioStatus status;

    /**
     * Papel de acesso do usuário.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @NotNull(message = "{usuario.role.not-null}")
    private UsuarioRole role;

    /**
     * Data de criação do registro (preenchida pela auditoria JPA).
     */
    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /**
     * Data da última atualização do registro (preenchida pela auditoria JPA).
     */
    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    /**
     * Administrador que aprovou ou rejeitou o cadastro.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private Usuario aprovadoPor;

    /**
     * Data em que o cadastro foi decidido (aprovado/rejeitado).
     */
    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    /**
     * Authorities expostas ao Spring Security. Mapeia {@link UsuarioRole} para o formato
     * esperado pelo {@code hasRole(...)} (prefixo {@code ROLE_}).
     */
    @Transient
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
