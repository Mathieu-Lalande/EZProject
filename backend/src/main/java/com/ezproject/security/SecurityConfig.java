package com.ezproject.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ezproject.ratelimit.LoginThrottle;
import com.ezproject.user.AppUserRepository;
import com.ezproject.ratelimit.RateLimitFilter;
import com.ezproject.ratelimit.RateLimitProperties;
import com.ezproject.ratelimit.RateLimiter;

/**
 * Authentification par session (cookie HttpOnly) + CSRF cookie-to-header pour la SPA React.
 * Choix volontaire plutôt qu'un JWT en localStorage : pas de token exposé au JavaScript (XSS).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; "
            + "img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimiter rateLimiter, LoginThrottle loginThrottle,
                                            RateLimitProperties rateLimits, AppUserRepository users,
                                            com.ezproject.observability.AppMetrics metrics,
                                            @Value("${app.metrics.token:}") String metricsToken) throws Exception {
        http
            .addFilterBefore(new com.ezproject.observability.MetricsTokenFilter(metricsToken),
                    org.springframework.security.web.context.SecurityContextHolderFilter.class)
            .addFilterAfter(new AccountStatusFilter(users), AuthorizationFilter.class)
            // Instancié ici (et non comme @Bean) pour qu'il ne soit pas enregistré une seconde fois comme filtre servlet
            .addFilterBefore(new RateLimitFilter(rateLimiter, loginThrottle, rateLimits, metrics),
                    UsernamePasswordAuthenticationFilter.class)
            .cors(cors -> { })
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .headers(headers -> headers
                // CSP stricte pour l'interface servie par le jar : aucun script inline ni domaine tiers hors polices
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
            .formLogin(form -> form
                // Page de connexion fournie par la SPA : désactive la page de login générée par Spring Security
                .loginPage("/login")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((req, res, auth) -> {
                    loginThrottle.recordSuccess(req.getParameter("email"));
                    boolean mfa = users.findByEmailIgnoreCase(auth.getName()).map(u -> u.isMfaEnabled()).orElse(false);
                    metrics.login(mfa ? "mfa_required" : "success");
                    if (mfa) {
                        // Mot de passe validé : la session reste bloquée jusqu'au second facteur
                        req.getSession().setAttribute(com.ezproject.mfa.MfaService.PENDING_ATTRIBUTE, Boolean.TRUE);
                        res.setStatus(HttpStatus.ACCEPTED.value());
                        res.setContentType("application/json");
                        res.getWriter().write("{\"mfaRequired\":true}");
                    } else {
                        res.setStatus(HttpStatus.NO_CONTENT.value());
                    }
                })
                .failureHandler((req, res, ex) -> {
                    loginThrottle.recordFailure(req.getParameter("email"));
                    metrics.login("failure");
                    res.sendError(HttpStatus.UNAUTHORIZED.value());
                }))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Délègue à bcrypt par défaut, avec préfixe d'algorithme ({bcrypt}) pour permettre une migration future
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
