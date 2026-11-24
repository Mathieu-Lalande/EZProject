package com.ezproject.audit;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.PageDto;
import com.ezproject.organization.Organization;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;

/**
 * Journal d'audit de l'organisation. Les services métier appellent {@link #record} après chaque action
 * significative ; la lecture est réservée aux administrateurs.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    public record AuditDto(Long id, String actorName, String action, String entityType, Long entityId, String summary,
                           Instant createdAt) {
    }

    private final AuditEventRepository repository;
    private final AppUserRepository users;

    public AuditService(AuditEventRepository repository, AppUserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    /** Enregistre une action de l'utilisateur connecté. */
    @Transactional
    public void record(String action, String entityType, Long entityId, String summary) {
        currentUser().ifPresent(u -> record(u.getOrganization(), u, action, entityType, entityId, summary));
    }

    /** Enregistre une action avec un acteur explicite (ou système si null). */
    @Transactional
    public void record(Organization org, AppUser actor, String action, String entityType, Long entityId,
                       String summary) {
        repository.save(new AuditEvent(org, actor, action, entityType, entityId, summary));
    }

    public PageDto<AuditDto> search(String entityType, String query, int page, int size) {
        AppUser me = currentUser().orElseThrow(() -> new AccessDeniedException("Non authentifié"));
        if (!me.getRole().isAdmin()) {
            throw new AccessDeniedException("Journal d'audit réservé aux administrateurs");
        }
        String like = query == null || query.isBlank() ? null : "%" + query.toLowerCase(Locale.ROOT).trim() + "%";
        var result = repository.search(me.getOrganization().getId(), blankToNull(entityType), like,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)));
        return PageDto.from(result, e -> new AuditDto(e.getId(), e.getActorName(), e.getAction(), e.getEntityType(),
                e.getEntityId(), e.getSummary(), e.getCreatedAt()));
    }

    private java.util.Optional<AppUser> currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? java.util.Optional.empty() : users.findByEmailIgnoreCase(auth.getName());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
