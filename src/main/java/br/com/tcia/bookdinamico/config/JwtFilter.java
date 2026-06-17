package br.com.tcia.bookdinamico.config;

import br.com.tcia.bookdinamico.entity.Usuario;
import br.com.tcia.bookdinamico.jwt.JwtTokenProvider;
import br.com.tcia.bookdinamico.repository.UsuarioRepository;
import br.com.tcia.bookdinamico.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Log4j2
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String JWT_REGEX = "^[A-Za-z0-9-_]+?\\.[A-Za-z0-9-_]+?\\.[A-Za-z0-9-_]+$";
    private static final Pattern JWT_PATTERN = Pattern.compile(JWT_REGEX);

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = obterToken(request);
        if (token != null && JWT_PATTERN.matcher(token).matches() && jwtTokenProvider.validarToken(token)) {
            String email = jwtTokenProvider.obterEmail(token);
            if (email != null) {
                Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
                if (usuario != null) {
                    autenticar(usuario);
                } else {
                    log.warn("JWT válido mas usuário não encontrado: {}", email);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    public String obterToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private void autenticar(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario, usuario.getAuthorities());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
