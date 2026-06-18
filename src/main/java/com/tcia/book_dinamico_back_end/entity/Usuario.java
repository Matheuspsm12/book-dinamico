package com.tcia.book_dinamico_back_end.entity;

import com.tcia.book_dinamico_back_end.enums.UsuarioStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um usuário do Book Dinâmico.
 *
 * <p>O ciclo de vida segue {@link UsuarioStatus} (PENDENTE → APROVADO/REJEITADO → DESATIVADO)
 * e o papel de acesso é definido pelo {@link Perfil} (base padrão TCIA). A auto-relação
 * {@code aprovadoPor} registra qual admin decidiu o cadastro.</p>
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
     * Perfil de acesso (base padrão TCIA). Substitui o antigo enum {@code role}.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_perfil", nullable = false)
    @NotNull(message = "{usuario.perfil.not-null}")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Perfil perfil;

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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario aprovadoPor;

    /**
     * Data em que o cadastro foi decidido (aprovado/rejeitado).
     */
    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    /**
     * Authorities expostas ao Spring Security (padrão TCIA): {@code ROLE_<nomePerfil>}
     * mais cada permissão do perfil.
     */
    @Transient
    public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (perfil != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + perfil.getNomePerfil().toUpperCase()));
            if (perfil.getPerfilPermissao() != null) {
                perfil.getPerfilPermissao().stream()
                        .filter(pp -> pp.getPermissao() != null)
                        .map(pp -> new SimpleGrantedAuthority(pp.getPermissao().getNomePermissao()))
                        .forEach(authorities::add);
            }
        }
        return authorities;
    }
}
