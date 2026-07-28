package com.tcia.book_dinamico_back_end.infrastructure.config;

import com.tcia.book_dinamico_back_end.infrastructure.security.JwtFilter;
import com.tcia.book_dinamico_back_end.infrastructure.security.JwtTokenProvider;
import com.tcia.book_dinamico_back_end.domain.repository.UsuarioRepository;
import com.tcia.book_dinamico_back_end.infrastructure.security.CustomAccessDeniedHandler;
import com.tcia.book_dinamico_back_end.infrastructure.security.CustomAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Log4j2
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.url-site}")
    private String urlSite;

    @Bean
    JwtFilter jwtFilter(JwtTokenProvider jwtTokenProvider, UsuarioRepository usuarioRepository) {
        return new JwtFilter(jwtTokenProvider, usuarioRepository);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter, JwtTokenProvider jwtTokenProvider,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/autenticacao/login").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/autenticacao/recuperar-senha").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/autenticacao/redefinir-senha").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/usuarios/cadastro").permitAll();
                    auth.anyRequest().authenticated();
                })
                .logout(logout -> logout
                        .logoutUrl("/autenticacao/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String token = jwtFilter.obterToken(request);
                            if (token != null) {
                                try {
                                    jwtTokenProvider.revogarToken(token);
                                } catch (Exception e) {
                                    log.error("Erro ao revogar token no logout: {}", e.getMessage());
                                }
                            }
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"mensagem\":\"Logoff realizado com sucesso.\"}");
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = Arrays.stream(urlSite.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .map(SecurityConfig::normalizarOrigem)
                .distinct()
                .toList();
        log.info("Origens permitidas (CORS): {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Client-Type", "X-Device-Id", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Normaliza a origem para o formato aceito pelo CORS: {@code scheme://host[:porta]},
     * removendo qualquer path, query ou barra final. Blinda o CORS contra valores como
     * {@code https://homol.book.tcia.com.br/book_dinamico/} — que como estão quebrariam
     * o match da origem enviada pelo navegador.
     */
    private static String normalizarOrigem(String valor) {
        try {
            URI uri = URI.create(valor);
            if (uri.getScheme() != null && uri.getHost() != null) {
                String origem = uri.getScheme() + "://" + uri.getHost();
                if (uri.getPort() != -1) {
                    origem += ":" + uri.getPort();
                }
                return origem;
            }
        } catch (IllegalArgumentException ignorado) {
            // valor não é URI válida; cai no fallback abaixo
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
