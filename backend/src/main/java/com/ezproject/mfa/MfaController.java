package com.ezproject.mfa;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.audit.AuditService;
import com.ezproject.ratelimit.RateLimitProperties;
import com.ezproject.ratelimit.RateLimiter;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class MfaController {

    private final MfaService service;
    private final UserService userService;
    private final RateLimiter limiter;
    private final RateLimitProperties limits;
    private final AuditService audit;

    public MfaController(MfaService service, UserService userService, RateLimiter limiter, RateLimitProperties limits,
                         AuditService audit) {
        this.service = service;
        this.userService = userService;
        this.limiter = limiter;
        this.limits = limits;
        this.audit = audit;
    }

    public record CodeRequest(@NotBlank @Size(max = 20) String code) {
    }

    public record PasswordRequest(@NotBlank @Size(max = 128) String password) {
    }

    @PostMapping("/api/account/mfa/setup")
    public MfaService.Setup setup() {
        return service.setup();
    }

    @PostMapping("/api/account/mfa/enable")
    public MfaService.RecoveryCodes enable(@Valid @RequestBody CodeRequest request) {
        return service.enable(request.code());
    }

    @DeleteMapping("/api/account/mfa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@Valid @RequestBody PasswordRequest request) {
        service.disable(request.password());
    }

    @DeleteMapping("/api/users/{userId}/mfa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminReset(@PathVariable Long userId) {
        service.adminReset(userId);
    }

    /**
     * Second facteur : valide la session (mot de passe déjà vérifié) si le code est bon.
     * 5 essais par 15 minutes (limite des échecs de connexion), puis la session est fermée.
     */
    @PostMapping("/api/auth/mfa/verify")
    public ResponseEntity<Void> verify(@Valid @RequestBody CodeRequest request, HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session == null || session.getAttribute(MfaService.PENDING_ATTRIBUTE) == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        AppUser user = userService.getByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        RateLimiter.Decision allowed = limiter.peek("mfa", limits.loginAccount(), user.getEmail());
        if (!allowed.allowed()) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(allowed.retryAfterSeconds())).build();
        }
        if (!service.verifyLogin(user, request.code())) {
            limiter.tryConsume("mfa", limits.loginAccount(), user.getEmail());
            audit.record(user.getOrganization(), user, "MFA_FAILED", "USER", user.getId(), "Code de double authentification incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        limiter.reset("mfa", user.getEmail());
        session.removeAttribute(MfaService.PENDING_ATTRIBUTE);
        http.changeSessionId();
        audit.record(user.getOrganization(), user, "MFA_VERIFIED", "USER", user.getId(), "Connexion à deux facteurs");
        return ResponseEntity.noContent().build();
    }
}
