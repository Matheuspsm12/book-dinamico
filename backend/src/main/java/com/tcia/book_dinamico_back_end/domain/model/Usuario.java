package com.tcia.book_dinamico_back_end.domain.model;

import com.tcia.book_dinamico_back_end.core.enums.UsuarioStatus;
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
    @NotBlank(message = "Nome não pode estar vazio!")
    @Size(max = 200, message = "Nome deve ter no máximo {max} caracteres!")
    private String nome;

    @Column(name = "empresa", nullable = false, length = 200)
    @NotBlank(message = "Empresa não pode estar vazia!")
    @Size(max = 200, message = "Empresa deve ter no máximo {max} caracteres!")
    private String empresa;

    @Column(name = "email", nullable = false, length = 200, unique = true)
    @NotBlank(message = "E-mail não pode estar vazio!")
    @Email(message = "E-mail inválido!")
    @Size(max = 200, message = "E-mail deve ter no máximo {max} caracteres!")
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    @NotBlank(message = "Senha não pode estar vazia!")
    private String senhaHash;

    @Column(name = "justificativa", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Justificativa não pode estar vazia!")
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status não pode ser nulo!")
    private UsuarioStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_perfil", nullable = false)
    @NotNull(message = "Perfil não pode ser nulo!")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Perfil perfil;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario aprovadoPor;

    @Column(name = "decidido_em")
    private LocalDateTime decididoEm;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    @Column(name = "ociosidade_notificado_em")
    private LocalDateTime ociosidadeNotificadoEm;

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
