package br.com.tcia.bookdinamico.security;

import br.com.tcia.bookdinamico.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Wrapper sobre {@link Usuario} para integrar com Spring Security.
 * Padrão TCIA: estende {@link User} e mantém a entidade acessível via {@link #getUsuario()}.
 */
public class CustomUserDetails extends User {

    private static final long serialVersionUID = 1L;

    @Getter
    private final transient Usuario usuario;

    public CustomUserDetails(Usuario usuario, Collection<? extends GrantedAuthority> authorities) {
        super(usuario.getEmail(), usuario.getSenhaHash(), authorities);
        this.usuario = usuario;
    }
}
