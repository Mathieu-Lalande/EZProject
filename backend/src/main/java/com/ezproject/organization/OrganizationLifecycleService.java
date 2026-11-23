package com.ezproject.organization;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.account.OrganizationDataContributor;
import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;

/** Portabilité (export complet) et suppression définitive d'une organisation (RGPD, résiliation). */
@Service
@Transactional(readOnly = true)
public class OrganizationLifecycleService {

    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final List<OrganizationDataContributor> contributors;
    private final EntityManager entityManager;

    public OrganizationLifecycleService(CurrentUser currentUser, JdbcTemplate jdbc, AuditService audit,
                                        List<OrganizationDataContributor> contributors, EntityManager entityManager) {
        this.entityManager = entityManager;
        this.currentUser = currentUser;
        this.jdbc = jdbc;
        this.audit = audit;
        this.contributors = contributors;
    }

    /** Export JSON de toutes les données de l'organisation (sans hash de mot de passe ni secret). */
    public Map<String, Object> export() {
        AppUser me = currentUser.get();
        if (!me.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
        Organization org = me.getOrganization();
        Long id = org.getId();
        // Les requêtes SQL directes doivent voir les écritures JPA en attente de la transaction courante
        entityManager.flush();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", Instant.now());
        data.put("organization", Map.of("name", org.getName(), "slug", org.getSlug(), "plan", org.getPlan()));
        data.put("users", rows("""
                SELECT id, email, display_name, role, job_title, active, created_at
                FROM app_user WHERE organization_id = ? AND deleted_at IS NULL ORDER BY id""", id));
        data.put("teams", rows("SELECT id, name, description, color FROM team WHERE organization_id = ?", id));
        data.put("teamMembers", rows("""
                SELECT tm.team_id, tm.user_id, tm.role FROM team_member tm
                JOIN team t ON t.id = tm.team_id WHERE t.organization_id = ?""", id));
        data.put("projects", rows("""
                SELECT id, code, name, description, client, type, status, status_reason, priority, health, visibility,
                       team_id, owner_id, start_date, end_date, budget_days, created_at FROM project
                WHERE organization_id = ? ORDER BY id""", id));
        data.put("tasks", rows("""
                SELECT t.id, t.project_id, t.title, t.description, t.status, t.priority, t.assignee_id, t.story_points,
                       t.due_date, t.created_at, t.completed_at FROM task t
                JOIN project p ON p.id = t.project_id WHERE p.organization_id = ? ORDER BY t.id""", id));
        data.put("projectTodos", rows("""
                SELECT td.id, td.project_id, td.title, td.done, td.due_date FROM todo_item td
                JOIN project p ON p.id = td.project_id WHERE p.organization_id = ?""", id));
        data.put("retroPhases", rows("""
                SELECT r.id, r.project_id, r.name, r.kind, r.duration_days, r.sort_order, r.done FROM retro_phase r
                JOIN project p ON p.id = r.project_id WHERE p.organization_id = ?""", id));
        data.put("statusHistory", rows("""
                SELECT s.project_id, s.from_status, s.to_status, s.reason, s.changed_at FROM project_status_change s
                JOIN project p ON p.id = s.project_id WHERE p.organization_id = ?""", id));
        contributors.forEach(c -> data.put(c.key(), c.export(org)));
        audit.record("ORGANIZATION_EXPORTED", "ORGANIZATION", id, "Export complet des données");
        return data;
    }

    /** Lignes SQL avec des clés en minuscules (H2 les renvoie en majuscules, PostgreSQL en minuscules). */
    private List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.queryForList(sql, args).stream()
                .map(row -> {
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    row.forEach((k, v) -> normalized.put(k.toLowerCase(java.util.Locale.ROOT), v));
                    return normalized;
                })
                .toList();
    }

    /**
     * Suppression définitive : projets (et tout leur contenu), comptes, équipes, journal.
     * Réservée au propriétaire, avec confirmation par le nom exact de l'organisation.
     */
    @Transactional
    public void delete(String confirmationName) {
        AppUser me = currentUser.get();
        if (me.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Réservé au propriétaire de l'organisation");
        }
        Organization org = me.getOrganization();
        if (confirmationName == null || !confirmationName.trim().equals(org.getName())) {
            throw new BusinessRuleException("Saisissez exactement le nom de l'organisation pour confirmer");
        }
        Long id = org.getId();
        entityManager.flush();
        // Ordre imposé par les clés étrangères : les projets (cascade sur leur contenu), puis les comptes,
        // puis l'organisation (cascade sur équipes, permissions et journal d'audit).
        jdbc.update("DELETE FROM project WHERE organization_id = ?", id);
        jdbc.update("DELETE FROM app_user WHERE organization_id = ?", id);
        jdbc.update("DELETE FROM organization WHERE id = ?", id);
    }
}
