package com.tcia.book_dinamico_back_end.utils;

import com.tcia.book_dinamico_back_end.entity.Usuario;
import com.tcia.book_dinamico_back_end.enums.UsuarioRole;
import com.tcia.book_dinamico_back_end.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component("authUtils")
public class AuthUtils {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public AuthUtils(@Lazy UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Retorna o usuário logado a partir do {@link SecurityContextHolder}.
     * Retorna {@code null} se não houver autenticação válida.
     */
    public Usuario getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            String email = user.getUsername();
            return usuarioRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    public Long getIdUsuarioLogado() {
        Usuario usuario = getUsuarioLogado();
        return usuario != null ? usuario.getId() : null;
    }

    public boolean isUsuarioLogadoAdmin() {
        Usuario usuario = getUsuarioLogado();
        return usuario != null && usuario.getRole() == UsuarioRole.ADMIN;
    }
}
