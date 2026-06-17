package br.com.tcia.bookdinamico.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitário para extrair o IP real do cliente, levando em conta proxies/load balancers.
 */
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
