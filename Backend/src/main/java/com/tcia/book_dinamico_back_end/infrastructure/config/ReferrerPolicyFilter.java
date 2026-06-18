package com.tcia.book_dinamico_back_end.infrastructure.config;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ReferrerPolicyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
