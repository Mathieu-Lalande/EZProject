package com.ezproject.user;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.organization.Organization;

/**
 * Utilisateurs, toujours lus et modifiés dans le périmètre de l'organisation de l'utilisateur connecté.
 * L'utilisateur connecté est résolu ici (et non via CurrentUser) pour éviter une dépendance circulaire.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder, AuditService audit) {
        this.audit = audit;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("Utilisateur", email));
    }

    public AppUser me() {
        return getByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    public List<UserDto> findAll() {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderByDisplayNameAsc(me().getOrganization().getId())
                .stream().map(UserDto::from).toList();
    }

    /** Utilisateur de l'organisation donnée ; 404 pour un utilisateur d'un autre locataire. */
    public AppUser getInOrganization(Long id, Long organizationId) {
        return repository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Utilisateur", id));
    }

    public java.util.Optional<AppUser> findByEmailInOrganization(String email, Long organizationId) {
        return repository.findByEmailIgnoreCaseAndOrganizationId(email, organizationId);
    }

    /** Utilisateurs comptés dans le quota de l'offre : actifs et non supprimés. */
    public long countInOrganization(Long organizationId) {
        return repository.countByOrganizationIdAndActiveTrueAndDeletedAtIsNull(organizationId);
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        AppUser me = me();
        requireAdmin(me);
        if (request.role() == Role.OWNER && me.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Seul un propriétaire peut nommer un autre propriétaire");
        }
        Organization org = me.getOrganization();
        if (!org.getPlan().allowsMoreUsers(countInOrganization(org.getId()))) {
            throw new BusinessRuleException("Limite d'utilisateurs atteinte pour l'offre " + org.getPlan());
        }
        AppUser created = register(org, request.email(), request.displayName(), request.password(), request.role(),
                request.jobTitle());
        audit.record("USER_CREATED", "USER", created.getId(), "Création du compte " + created.getEmail() + " (" + created.getRole() + ")");
        return UserDto.from(created);
    }

    /** Création brute, utilisée aussi par l'inscription self-service et l'initialisation. */
    @Transactional
    public AppUser register(Organization org, String email, String displayName, String rawPassword, Role role,
                            String jobTitle) {
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Un compte existe déjà avec cet email");
        }
        com.ezproject.account.PasswordPolicy.check(rawPassword, email);
        return repository.save(new AppUser(org, email, displayName, passwordEncoder.encode(rawPassword), role, jobTitle));
    }

    @Transactional
    public UserDto changeRole(Long userId, Role newRole) {
        AppUser me = me();
        requireAdmin(me);
        AppUser target = getInOrganization(userId, me.getOrganization().getId());
        if (target.getId().equals(me.getId())) {
            throw new BusinessRuleException("Vous ne pouvez pas modifier votre propre rôle");
        }
        boolean touchesOwner = newRole == Role.OWNER || target.getRole() == Role.OWNER;
        if (touchesOwner && me.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Seul un propriétaire peut gérer le rôle propriétaire");
        }
        audit.record("USER_ROLE", "USER", target.getId(), target.getDisplayName() + " : " + target.getRole() + " → " + newRole);
        target.changeRole(newRole);
        return UserDto.from(target);
    }

    private static void requireAdmin(AppUser user) {
        if (!user.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
    }
}
