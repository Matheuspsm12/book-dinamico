package com.tcia.book_dinamico_back_end.core.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RecuperarIpUtils {

    private static final String[] HEADERS_PROXY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
    };

    private RecuperarIpUtils() {
    }

    public static String obterIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        for (String header : HEADERS_PROXY) {
            String valor = request.getHeader(header);
            if (valor != null && !valor.isBlank() && !"unknown".equalsIgnoreCase(valor)) {
                return valor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
