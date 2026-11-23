package com.ezproject.security;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gestion CSRF pour une SPA (pattern "cookie-to-header") tel que décrit dans la documentation
 * Spring Security, section "Single-Page Applications" :
 * le token est publié dans le cookie XSRF-TOKEN, le front le renvoie dans l'en-tête X-XSRF-TOKEN.
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // Protection BREACH pour les tokens rendus dans une réponse
        this.xor.handle(request, response, csrfToken);
        // Force le chargement du token pour que le cookie soit émis à chaque réponse
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        // En-tête envoyé par la SPA (valeur brute du cookie) -> handler "plain" ; sinon paramètre masqué -> XOR
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
