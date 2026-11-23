package com.ezproject.dataexchange;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.ezproject.access.Visibility;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.Health;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectPriority;
import com.ezproject.project.ProjectRepository;
import com.ezproject.project.ProjectRequest;
import com.ezproject.project.ProjectService;
import com.ezproject.project.ProjectStatus;
import com.ezproject.project.ProjectType;
import com.ezproject.security.CurrentUser;
import com.ezproject.task.Priority;
import com.ezproject.task.TaskRequest;
import com.ezproject.task.TaskService;
import com.ezproject.task.TaskStatus;
import com.ezproject.team.Team;
import com.ezproject.team.TeamRepository;
import com.ezproject.team.TeamRequest;
import com.ezproject.team.TeamService;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Import CSV de données existantes (portefeuille tenu sous Excel, export d'un autre outil…).
 * <p>
 * Chaque ligne passe par les services métier (mêmes règles, mêmes autorisations que l'interface).
 * L'import est transactionnel et "tout ou rien" : la moindre erreur annule l'ensemble ;
 * en simulation, tout est exécuté puis annulé, ce qui donne un rapport exactement fidèle.
 */
@Service
public class ImportService {

    private static final Map<String, ProjectStatus> STATUS = Map.of(
            "planifie", ProjectStatus.PLANNED, "a_venir", ProjectStatus.PLANNED, "en_cours", ProjectStatus.ACTIVE,
            "actif", ProjectStatus.ACTIVE, "en_pause", ProjectStatus.ON_HOLD, "on_hold", ProjectStatus.ON_HOLD,
            "suspendu", ProjectStatus.ON_HOLD, "termine", ProjectStatus.COMPLETED, "clos", ProjectStatus.COMPLETED,
            "annule", ProjectStatus.CANCELLED);
    private static final Map<String, ProjectType> TYPES = Map.of(
            "developpement", ProjectType.DEVELOPMENT, "dev", ProjectType.DEVELOPMENT, "conseil", ProjectType.CONSULTING,
            "consulting", ProjectType.CONSULTING, "interne", ProjectType.INTERNAL);
    private static final Map<String, Health> HEALTH = Map.of(
            "vert", Health.GREEN, "orange", Health.AMBER, "rouge", Health.RED, "sous_controle", Health.GREEN,
            "vigilance", Health.AMBER, "critique", Health.RED);
    private static final Map<String, ProjectPriority> PRIORITY = Map.of(
            "1", ProjectPriority.P1, "2", ProjectPriority.P2, "3", ProjectPriority.P3, "4", ProjectPriority.P4,
            "strategique", ProjectPriority.P1, "critique", ProjectPriority.P1, "haute", ProjectPriority.P2, "normale", ProjectPriority.P3,
            "basse", ProjectPriority.P4);
    private static final Map<String, Visibility> VISIBILITY = Map.of(
            "organisation", Visibility.ORGANIZATION, "equipe", Visibility.TEAM, "restreint", Visibility.RESTRICTED,
            "confidentiel", Visibility.RESTRICTED);
    private static final Map<String, TaskStatus> TASK_STATUS = Map.of(
            "a_faire", TaskStatus.TODO, "en_cours", TaskStatus.IN_PROGRESS, "en_revue", TaskStatus.REVIEW,
            "termine", TaskStatus.DONE, "fait", TaskStatus.DONE);
    private static final Map<String, Priority> TASK_PRIORITY = Map.of(
            "basse", Priority.LOW, "moyenne", Priority.MEDIUM, "normale", Priority.MEDIUM, "haute", Priority.HIGH,
            "critique", Priority.CRITICAL);

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final TaskService taskService;
    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final Validator validator;
    private final TransactionTemplate transaction;

    public ImportService(ProjectService projectService, ProjectRepository projectRepository, TaskService taskService,
                         TeamService teamService, TeamRepository teamRepository, UserService userService,
                         CurrentUser currentUser, Validator validator, PlatformTransactionManager txManager) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.taskService = taskService;
        this.teamService = teamService;
        this.teamRepository = teamRepository;
        this.userService = userService;
        this.currentUser = currentUser;
        this.validator = validator;
        this.transaction = new TransactionTemplate(txManager);
    }

    /** Colonnes : code*, nom*, type, statut, motif_statut, priorite, meteo, client, equipe, responsable (email),
     *  visibilite, debut, echeance, budget_jours, description. Upsert par code. */
    public ImportReport importProjects(InputStream csv, boolean dryRun) {
        AppUser me = currentUser.get();
        if (!me.getRole().seesWholePortfolio()) {
            throw new AccessDeniedException("L'import de projets est réservé aux managers et administrateurs");
        }
        List<CsvTable.Row> rows = CsvTable.parse(csv);
        Long orgId = me.getOrganization().getId();
        return transaction.execute(tx -> {
            Counters c = new Counters();
            Set<String> seenCodes = new HashSet<>();
            for (CsvTable.Row row : rows) {
                try {
                    String code = upper(row.get("code", "code_projet"));
                    if (code != null && !seenCodes.add(code)) {
                        throw new IllegalArgumentException("Code " + code + " présent plusieurs fois dans le fichier");
                    }
                    Long teamId = resolveTeam(row.get("equipe", "team"), orgId, c);
                    Long ownerId = resolveUser(row.get("responsable", "owner", "owner_email", "email_responsable"), orgId);
                    ProjectRequest request = new ProjectRequest(code, row.get("nom", "name", "projet"),
                            row.get("description"), row.get("client"),
                            orDefault(ValueParser.enumValue(ProjectType.class, row.get("type"), TYPES, "Type"),
                                    ProjectType.DEVELOPMENT),
                            ValueParser.enumValue(ProjectStatus.class, row.get("statut", "status"), STATUS, "Statut"),
                            row.get("motif_statut", "motif", "status_reason"),
                            ValueParser.enumValue(ProjectPriority.class, row.get("priorite", "priority"), PRIORITY, "Priorité"),
                            ValueParser.enumValue(Health.class, row.get("meteo", "health", "sante"), HEALTH, "Météo"),
                            null, ownerId, teamId,
                            ValueParser.enumValue(Visibility.class, row.get("visibilite", "visibility"), VISIBILITY, "Visibilité"),
                            ValueParser.date(row.get("debut", "date_debut", "start_date"), "Début"),
                            ValueParser.date(row.get("echeance", "fin", "date_fin", "end_date"), "Échéance"),
                            ValueParser.integer(row.get("budget_jours", "budget", "budget_days"), "Budget"));
                    validate(request);
                    Optional<Project> existing = projectRepository.findByOrganizationIdAndCodeIgnoreCase(orgId, code);
                    if (existing.isPresent()) {
                        Project p = existing.get();
                        Long currentTeamId = p.getTeam() == null ? null : p.getTeam().getId();
                        // Mise à jour partielle : une colonne vide conserve la valeur existante
                        projectService.update(p.getId(), new ProjectRequest(p.getCode(), request.name(),
                                coalesce(request.description(), p.getDescription()), coalesce(request.client(), p.getClient()),
                                row.get("type") == null ? p.getType() : request.type(), request.status(),
                                request.statusReason(), request.priority(), request.health(), p.getColor(),
                                ownerId != null ? ownerId : (p.getOwner() == null ? null : p.getOwner().getId()),
                                teamId != null ? teamId : currentTeamId, request.visibility(),
                                coalesce(request.startDate(), p.getStartDate()), coalesce(request.endDate(), p.getEndDate()),
                                coalesce(request.budgetDays(), p.getBudgetDays())));
                        c.updated++;
                        c.preview.add("Ligne " + row.line() + " : mise à jour de " + code);
                    } else {
                        projectService.create(request);
                        c.created++;
                        c.preview.add("Ligne " + row.line() + " : création de " + code + " — " + request.name());
                    }
                } catch (RuntimeException e) {
                    c.errors.add(new ImportReport.RowError(row.line(), messageOf(e)));
                }
            }
            return finish(tx, dryRun, rows.size(), c);
        });
    }

    /** Colonnes : projet (code)*, titre*, statut, priorite, assigne (email), points, echeance, description. */
    public ImportReport importTasks(InputStream csv, boolean dryRun) {
        AppUser me = currentUser.get();
        List<CsvTable.Row> rows = CsvTable.parse(csv);
        Long orgId = me.getOrganization().getId();
        return transaction.execute(tx -> {
            Counters c = new Counters();
            for (CsvTable.Row row : rows) {
                try {
                    String code = upper(row.get("projet", "code_projet", "project", "project_code"));
                    if (code == null) {
                        throw new IllegalArgumentException("Colonne projet (code) obligatoire");
                    }
                    Project project = projectRepository.findByOrganizationIdAndCodeIgnoreCase(orgId, code)
                            .orElseThrow(() -> new IllegalArgumentException("Projet inconnu : " + code));
                    TaskRequest request = new TaskRequest(row.get("titre", "title", "tache"), row.get("description"),
                            ValueParser.enumValue(TaskStatus.class, row.get("statut", "status"), TASK_STATUS, "Statut"),
                            ValueParser.enumValue(Priority.class, row.get("priorite", "priority"), TASK_PRIORITY, "Priorité"),
                            resolveUser(row.get("assigne", "assignee", "assignee_email", "responsable"), orgId),
                            ValueParser.integer(row.get("points", "story_points"), "Points"),
                            ValueParser.date(row.get("echeance", "due_date", "date"), "Échéance"));
                    validate(request);
                    taskService.create(project.getId(), request);
                    c.created++;
                    c.preview.add("Ligne " + row.line() + " : " + code + " — " + request.title());
                } catch (RuntimeException e) {
                    c.errors.add(new ImportReport.RowError(row.line(), messageOf(e)));
                }
            }
            return finish(tx, dryRun, rows.size(), c);
        });
    }

    private ImportReport finish(org.springframework.transaction.TransactionStatus tx, boolean dryRun, int rows,
                                Counters c) {
        boolean applied = !dryRun && c.errors.isEmpty();
        if (!applied) {
            tx.setRollbackOnly();
        }
        return new ImportReport(dryRun, applied, rows, c.created, c.updated, c.teamsCreated, List.copyOf(c.errors),
                c.preview.stream().limit(50).toList());
    }

    private Long resolveTeam(String name, Long orgId, Counters c) {
        if (name == null) {
            return null;
        }
        Optional<Team> team = teamRepository.findByOrganizationIdAndNameIgnoreCase(orgId, name);
        if (team.isPresent()) {
            return team.get().getId();
        }
        c.teamsCreated++;
        return teamService.create(new TeamRequest(name, null, null)).id();
    }

    private Long resolveUser(String email, Long orgId) {
        if (email == null) {
            return null;
        }
        return userService.findByEmailInOrganization(email, orgId).map(AppUser::getId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu dans l'organisation : " + email));
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.stream()
                    .map(v -> v.getPropertyPath() + " : " + v.getMessage()).sorted().collect(Collectors.joining(", ")));
        }
    }

    /** Messages d'erreur métier exposés tels quels ; erreurs techniques masquées (pas de fuite d'information). */
    private static String messageOf(RuntimeException e) {
        if (e instanceof IllegalArgumentException || e instanceof BusinessRuleException
                || e instanceof NotFoundException) {
            return e.getMessage();
        }
        if (e instanceof AccessDeniedException) {
            return "Droits insuffisants sur ce projet";
        }
        return "Ligne invalide";
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(java.util.Locale.ROOT);
    }

    private static <T> T orDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static <T> T coalesce(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static final class Counters {
        int created;
        int updated;
        int teamsCreated;
        final List<ImportReport.RowError> errors = new ArrayList<>();
        final List<String> preview = new ArrayList<>();
    }
}
