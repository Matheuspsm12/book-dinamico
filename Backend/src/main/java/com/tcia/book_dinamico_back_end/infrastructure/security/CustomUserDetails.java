package com.tcia.book_dinamico_back_end.infrastructure.security;

import com.tcia.book_dinamico_back_end.domain.model.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    private static final long serialVersionUID = 1L;

    @Getter
    private final transient Usuario usuario;

    public CustomUserDetails(Usuario usuario, Collection<? extends GrantedAuthority> authorities) {
        super(usuario.getEmail(), usuario.getSenhaHash(), authorities);
        this.usuario = usuario;
    }
}
