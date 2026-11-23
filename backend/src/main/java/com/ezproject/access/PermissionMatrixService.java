package com.ezproject.access;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.organization.Organization;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;

@Service
@Transactional(readOnly = true)
public class PermissionMatrixService {

    private final RolePermissionRepository repository;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public PermissionMatrixService(RolePermissionRepository repository, CurrentUser currentUser, AuditService audit) {
        this.audit = audit;
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public PermissionMatrix matrixOf(Organization org) {
        Map<ProjectRole, Set<Permission>> roles = new EnumMap<>(ProjectRole.class);
        for (ProjectRole r : ProjectRole.values()) {
            roles.put(r, org.hasCustomPermissions() ? EnumSet.noneOf(Permission.class) : Permission.defaultsFor(r));
        }
        if (org.hasCustomPermissions()) {
            repository.findByOrganization(org.getId())
                    .forEach(rp -> roles.get(rp.getRole()).add(rp.getPermission()));
        }
        return new PermissionMatrix(org.hasCustomPermissions(), roles);
    }

    public PermissionMatrix currentMatrix() {
        return matrixOf(currentUser.get().getOrganization());
    }

    @Transactional
    public PermissionMatrix update(Map<ProjectRole, Set<Permission>> roles) {
        AppUser me = currentUser.get();
        requireAdmin(me);
        Organization org = me.getOrganization();
        if (!org.getPlan().allowsCustomPermissions()) {
            throw new BusinessRuleException("La personnalisation des permissions nécessite l'offre TEAM ou ENTERPRISE");
        }
        // Garde-fou : le responsable de projet doit pouvoir consulter et gérer les accès, sinon le projet devient ingérable
        Set<Permission> lead = roles.getOrDefault(ProjectRole.LEAD, Set.of());
        if (!lead.contains(Permission.PROJECT_MEMBERS) || !lead.contains(Permission.TASK_VIEW)) {
            throw new BusinessRuleException("Le rôle Responsable doit conserver la consultation et la gestion des membres");
        }
        repository.deleteByOrganization(org.getId());
        repository.flush();
        roles.forEach((role, perms) -> perms.forEach(p -> repository.save(new RolePermission(org.getId(), role, p))));
        org.markPermissionsCustomized(true);
        audit.record("PERMISSIONS_UPDATED", "ORGANIZATION", org.getId(), "Matrice de permissions personnalisée");
        return matrixOf(org);
    }

    @Transactional
    public PermissionMatrix reset() {
        AppUser me = currentUser.get();
        requireAdmin(me);
        repository.deleteByOrganization(me.getOrganization().getId());
        me.getOrganization().markPermissionsCustomized(false);
        audit.record("PERMISSIONS_RESET", "ORGANIZATION", me.getOrganization().getId(), "Matrice par défaut restaurée");
        return matrixOf(me.getOrganization());
    }

    private static void requireAdmin(AppUser user) {
        if (!user.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
    }
}
