package com.ezproject.observability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protège /actuator/prometheus par un jeton (Authorization: Bearer METRICS_TOKEN), comparé à temps constant.
 * Sans jeton configuré, l'endpoint répond 404 : aucune métrique n'est exposée par défaut.
 */
public class MetricsTokenFilter extends OncePerRequestFilter {

    private final byte[] token;

    public MetricsTokenFilter(String token) {
        this.token = token == null || token.isBlank() ? null : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/actuator/prometheus");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (token == null) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        String header = request.getHeader("Authorization");
        byte[] given = header != null && header.startsWith("Bearer ")
                ? header.substring(7).getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (!MessageDigest.isEqual(token, given)) {
            response.setHeader("WWW-Authenticate", "Bearer");
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        chain.doFilter(request, response);
    }
}
