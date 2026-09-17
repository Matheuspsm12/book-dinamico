package com.tcia.book_dinamico_back_end.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final int MAX_CORRELATION_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = normalizar(request.getHeader(CORRELATION_ID_HEADER));

        MDC.put(TRACE_ID, traceId);
        response.setHeader(CORRELATION_ID_HEADER, traceId);
        response.setHeader(TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String limpo = valor.trim();
        return limpo.length() > MAX_CORRELATION_LENGTH
                ? limpo.substring(0, MAX_CORRELATION_LENGTH)
                : limpo;
    }
}
