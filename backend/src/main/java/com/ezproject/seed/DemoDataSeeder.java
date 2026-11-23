package com.ezproject.seed;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import com.ezproject.access.ProjectMember;
import com.ezproject.calendar.HolidayCalendar;
import com.ezproject.capacity.Absence;
import com.ezproject.capacity.AbsenceKind;
import com.ezproject.capacity.AbsenceRepository;
import com.ezproject.capacity.Allocation;
import com.ezproject.capacity.AllocationRepository;
import com.ezproject.time.TimeEntry;
import com.ezproject.time.TimeEntryRepository;
import com.ezproject.report.StatusReport;
import com.ezproject.report.StatusReportRepository;
import com.ezproject.report.Trend;
import com.ezproject.risk.Decision;
import com.ezproject.risk.DecisionRepository;
import com.ezproject.risk.DecisionStatus;
import com.ezproject.risk.Risk;
import com.ezproject.risk.RiskRepository;
import com.ezproject.risk.RiskStatus;
import com.ezproject.access.ProjectMemberRepository;
import com.ezproject.access.ProjectRole;
import com.ezproject.access.Visibility;
import com.ezproject.organization.Organization;
import com.ezproject.organization.OrganizationRepository;
import com.ezproject.organization.Plan;
import com.ezproject.poker.PokerRound;
import com.ezproject.poker.PokerRoundRepository;
import com.ezproject.project.Health;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectPriority;
import com.ezproject.project.ProjectRepository;
import com.ezproject.project.ProjectStatus;
import com.ezproject.project.ProjectStatusChange;
import com.ezproject.project.ProjectStatusChangeRepository;
import com.ezproject.project.ProjectType;
import com.ezproject.retroplanning.PhaseKind;
import com.ezproject.retroplanning.RetroPhase;
import com.ezproject.retroplanning.RetroPhaseRepository;
import com.ezproject.task.Priority;
import com.ezproject.task.Task;
import com.ezproject.task.TaskRepository;
import com.ezproject.task.TaskStatus;
import com.ezproject.team.Team;
import com.ezproject.team.TeamMembership;
import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.team.TeamRepository;
import com.ezproject.todo.TodoItem;
import com.ezproject.todo.TodoRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;
import com.ezproject.user.Role;

/**
 * Jeu de données de démonstration (app.seed.enabled=true, profil dev) :
 * deux organisations pour illustrer l'isolation multi-tenant, plusieurs équipes, un invité client,
 * des projets de priorités et statuts variés (dont un en pause et un confidentiel).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final TeamRepository teams;
    private final TeamMembershipRepository teamMembers;
    private final ProjectRepository projects;
    private final ProjectMemberRepository projectMembers;
    private final ProjectStatusChangeRepository statusChanges;
    private final TaskRepository tasks;
    private final TodoRepository todos;
    private final RetroPhaseRepository phases;
    private final PokerRoundRepository poker;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String demoPassword;
    private String hash;
    private TimeEntryRepository timeEntries;
    private AllocationRepository allocations;
    private AbsenceRepository absences;

    private RiskRepository risks;
    private DecisionRepository decisions;
    private StatusReportRepository reports;

    @org.springframework.beans.factory.annotation.Autowired
    void setSteeringRepositories(RiskRepository risks, DecisionRepository decisions, StatusReportRepository reports) {
        this.risks = risks;
        this.decisions = decisions;
        this.reports = reports;
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setCapacityRepositories(TimeEntryRepository timeEntries, AllocationRepository allocations,
                                 AbsenceRepository absences) {
        this.timeEntries = timeEntries;
        this.allocations = allocations;
        this.absences = absences;
    }

    DemoDataSeeder(OrganizationRepository organizations, AppUserRepository users, TeamRepository teams,
                   TeamMembershipRepository teamMembers, ProjectRepository projects,
                   ProjectMemberRepository projectMembers, ProjectStatusChangeRepository statusChanges,
                   TaskRepository tasks, TodoRepository todos, RetroPhaseRepository phases,
                   PokerRoundRepository poker, PasswordEncoder encoder, Clock clock,
                   @Value("${app.seed.demo-password}") String demoPassword) {
        this.organizations = organizations;
        this.users = users;
        this.teams = teams;
        this.teamMembers = teamMembers;
        this.projects = projects;
        this.projectMembers = projectMembers;
        this.statusChanges = statusChanges;
        this.tasks = tasks;
        this.todos = todos;
        this.phases = phases;
        this.poker = poker;
        this.encoder = encoder;
        this.clock = clock;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        hash = encoder.encode(demoPassword);
        LocalDate today = LocalDate.now(clock);

        Organization horizon = organizations.save(new Organization("Horizon Conseil", "horizon", Plan.TEAM));

        AppUser camille = user(horizon, "camille", "Camille Martin", Role.OWNER, "COO");
        AppUser lea = user(horizon, "lea", "Léa Dubois", Role.MANAGER, "PMO");
        AppUser hugo = user(horizon, "hugo", "Hugo Bernard", Role.MEMBER, "Tech lead Java");
        AppUser sarah = user(horizon, "sarah", "Sarah Petit", Role.MEMBER, "Développeuse React");
        AppUser thomas = user(horizon, "thomas", "Thomas Roux", Role.MEMBER, "Consultant SI senior");
        AppUser nina = user(horizon, "nina", "Nina Lefèvre", Role.MEMBER, "Consultante");
        AppUser julie = user(horizon, "julie", "Julie Moreau", Role.GUEST, "DSI adjointe — Banque Horizon (client)");

        Team delivery = team(horizon, "Delivery Web", "Équipe de développement Java / React", "#6366f1");
        Team conseil = team(horizon, "Conseil SI", "Missions CIO office, audit et stratégie SI", "#f59e0b");
        Team direction = team(horizon, "Direction", "COO, PMO et projets internes", "#10b981");
        member(delivery, hugo, ProjectRole.LEAD);
        member(delivery, sarah, ProjectRole.CONTRIBUTOR);
        member(conseil, thomas, ProjectRole.LEAD);
        member(conseil, nina, ProjectRole.CONTRIBUTOR);
        member(direction, camille, ProjectRole.LEAD);
        member(direction, lea, ProjectRole.LEAD);

        Project portail = project(horizon, delivery, "PORTAIL", "Portail client", ProjectType.DEVELOPMENT,
                "Banque Horizon", ProjectStatus.ACTIVE, null, ProjectPriority.P1, Health.GREEN, Visibility.TEAM,
                "#6366f1", lea, today.minusDays(40), today.plusDays(45), 180,
                "Refonte du portail client : authentification, tableau de bord et paiements.");
        Project mobile = project(horizon, delivery, "MOBILE", "App mobile terrain", ProjectType.DEVELOPMENT,
                "Énergies du Sud", ProjectStatus.ACTIVE, null, ProjectPriority.P2, Health.RED, Visibility.ORGANIZATION,
                "#ef4444", hugo, today.minusDays(60), today.plusDays(18), 120,
                "Application de saisie hors-ligne pour les techniciens.");
        Project audit = project(horizon, conseil, "AUDITSI", "Audit SI groupe", ProjectType.CONSULTING,
                "Mutuelle Atlas", ProjectStatus.ACTIVE, null, ProjectPriority.P1, Health.AMBER, Visibility.TEAM,
                "#f59e0b", thomas, today.minusDays(12), today.plusDays(28), 40,
                "Diagnostic du système d'information et feuille de route à 3 ans.");
        Project datalake = project(horizon, conseil, "DATA", "Cadrage data platform", ProjectType.CONSULTING,
                "Groupe Vauban", ProjectStatus.ON_HOLD, "Budget client gelé jusqu'au prochain trimestre",
                ProjectPriority.P3, Health.AMBER, Visibility.TEAM, "#0ea5e9", thomas, today.minusDays(30),
                today.plusDays(60), 25, "Étude d'opportunité d'une plateforme data.");
        Project rh = project(horizon, direction, "ONBOARD", "Onboarding RH", ProjectType.INTERNAL, null,
                ProjectStatus.PLANNED, null, ProjectPriority.P4, Health.GREEN, Visibility.ORGANIZATION, "#10b981",
                camille, today.plusDays(7), today.plusDays(50), 15,
                "Digitaliser le parcours d'arrivée des nouveaux collaborateurs.");
        Project manda = project(horizon, direction, "MANDA", "Due diligence acquisition", ProjectType.CONSULTING,
                null, ProjectStatus.ACTIVE, null, ProjectPriority.P1, Health.AMBER, Visibility.RESTRICTED, "#8b5cf6",
                camille, today.minusDays(5), today.plusDays(25), 30,
                "Projet confidentiel : seuls les membres explicitement ajoutés y ont accès.");

        // Accès explicites : client invité en lecture, consultant transverse sur le projet confidentiel
        projectMembers.save(new ProjectMember(portail, julie, ProjectRole.VIEWER));
        projectMembers.save(new ProjectMember(manda, thomas, ProjectRole.CONTRIBUTOR));

        Map<TaskStatus, Integer> pos = new EnumMap<>(TaskStatus.class);
        task(portail, pos, "Maquettes du tableau de bord", TaskStatus.DONE, Priority.HIGH, sarah, 5, today.minusDays(20));
        task(portail, pos, "API d'authentification OAuth2", TaskStatus.DONE, Priority.CRITICAL, hugo, 8, today.minusDays(10));
        Task paiement = task(portail, pos, "Intégration prestataire de paiement", TaskStatus.IN_PROGRESS, Priority.HIGH, hugo, null, today.plusDays(6));
        task(portail, pos, "Écran historique des opérations", TaskStatus.IN_PROGRESS, Priority.MEDIUM, sarah, 5, today.plusDays(9));
        task(portail, pos, "Tests de charge", TaskStatus.TODO, Priority.MEDIUM, hugo, 3, today.plusDays(20));
        task(portail, pos, "Accessibilité RGAA", TaskStatus.REVIEW, Priority.MEDIUM, sarah, 3, today.plusDays(2));
        task(portail, pos, "Export PDF des relevés", TaskStatus.BACKLOG, Priority.LOW, null, null, null);

        pos.clear();
        task(mobile, pos, "Synchronisation hors-ligne", TaskStatus.IN_PROGRESS, Priority.CRITICAL, hugo, 13, today.minusDays(5));
        task(mobile, pos, "Signature électronique", TaskStatus.TODO, Priority.HIGH, sarah, 8, today.minusDays(2));
        task(mobile, pos, "Publication stores", TaskStatus.TODO, Priority.MEDIUM, null, 3, today.plusDays(15));
        task(mobile, pos, "Formulaire d'intervention", TaskStatus.DONE, Priority.HIGH, sarah, 5, today.minusDays(15));

        pos.clear();
        task(audit, pos, "Entretiens DSI et métiers", TaskStatus.DONE, Priority.HIGH, thomas, null, today.minusDays(3));
        task(audit, pos, "Cartographie applicative", TaskStatus.IN_PROGRESS, Priority.HIGH, nina, null, today.minusDays(1));
        task(audit, pos, "Analyse des coûts d'infrastructure", TaskStatus.TODO, Priority.MEDIUM, nina, null, today.plusDays(8));
        task(audit, pos, "Préparer support COPIL", TaskStatus.TODO, Priority.HIGH, thomas, null, today.plusDays(25));

        pos.clear();
        task(datalake, pos, "Inventaire des sources de données", TaskStatus.DONE, Priority.MEDIUM, nina, null, today.minusDays(20));
        task(datalake, pos, "Benchmark des solutions", TaskStatus.TODO, Priority.MEDIUM, thomas, null, null);

        pos.clear();
        task(rh, pos, "Recenser les étapes actuelles", TaskStatus.TODO, Priority.MEDIUM, camille, null, today.plusDays(10));

        pos.clear();
        task(manda, pos, "Revue des contrats clients", TaskStatus.IN_PROGRESS, Priority.HIGH, thomas, null, today.plusDays(4));
        task(manda, pos, "Note de synthèse au comité", TaskStatus.TODO, Priority.CRITICAL, camille, null, today.plusDays(20));

        retro(portail, List.of(phase("Cadrage", 5, true), phase("Conception", 10, true), phase("Développement", 30, false),
                phase("Recette", 10, false), milestone("Go / No-Go"), phase("Mise en production", 2, false)));
        retro(mobile, List.of(phase("Cadrage", 5, true), phase("Développement", 25, false), phase("Recette", 8, false),
                milestone("Go / No-Go"), phase("Publication", 2, false)));
        retro(audit, List.of(milestone("Kick-off"), phase("Diagnostic", 10, true), phase("Ateliers", 10, false),
                phase("Recommandations", 8, false), milestone("Restitution COPIL")));

        todos.save(new TodoItem(camille, null, "Relire le reporting mensuel du portefeuille", today.plusDays(1)));
        todos.save(new TodoItem(camille, null, "Arbitrer le staffing de l'app mobile", today));
        todos.save(new TodoItem(lea, portail, "Envoyer le compte-rendu du comité de suivi", today.plusDays(2)));
        todos.save(new TodoItem(thomas, audit, "Réserver la salle pour les ateliers", today.plusDays(4)));

        poker.save(new PokerRound(portail, paiement, paiement.getTitle()));

        // Temps passés (jours ouvrés passés) : PORTAIL dans les clous, AUDITSI consomme plus vite qu'il n'avance
        logTime(portail, hugo, today, 30, "0.75");
        logTime(portail, sarah, today, 30, "0.5");
        logTime(mobile, hugo, today, 45, "0.25");
        logTime(mobile, sarah, today, 60, "0.5");
        logTime(audit, thomas, today, 12, "0.75");
        logTime(audit, nina, today, 12, "1");
        logTime(datalake, nina, today.minusDays(20), 10, "0.5");
        logTime(manda, thomas, today, 5, "0.25");

        // Staffing des 2 prochains mois : Hugo est surchargé (5,5 j/sem. pour 5 disponibles)
        allocate(portail, hugo, today.minusWeeks(2), today.plusWeeks(8), "3");
        allocate(mobile, hugo, today.minusWeeks(2), today.plusWeeks(4), "2.5");
        allocate(portail, sarah, today.minusWeeks(2), today.plusWeeks(8), "2.5");
        allocate(mobile, sarah, today.minusWeeks(2), today.plusWeeks(4), "2");
        allocate(audit, thomas, today.minusWeeks(2), today.plusWeeks(4), "3");
        allocate(manda, thomas, today.minusWeeks(1), today.plusWeeks(4), "1.5");
        allocate(audit, nina, today.minusWeeks(2), today.plusWeeks(4), "4");
        allocate(manda, camille, today.minusWeeks(1), today.plusWeeks(4), "1");
        nina.changeCapacity(new BigDecimal("4"));

        risk(mobile, "Synchronisation hors-ligne instable sur les vieux terminaux", 4, 4, RiskStatus.OPEN, hugo,
                "Banc de test sur 5 modèles, plan B : mode connecté dégradé", today.plusDays(7));
        risk(mobile, "Retard de validation des stores", 2, 3, RiskStatus.OPEN, sarah, "Soumission anticipée d'une bêta", null);
        risk(portail, "Délai d'homologation du prestataire de paiement", 2, 3, RiskStatus.MITIGATING, hugo,
                "Environnement bac à sable obtenu, relance hebdomadaire", today.plusDays(10));
        risk(audit, "Faible disponibilité des métiers pour les entretiens", 3, 3, RiskStatus.OPEN, thomas,
                "Créneaux réservés via la DSI, ateliers groupés", today.plusDays(5));
        risk(audit, "Périmètre applicatif sous-estimé", 2, 2, RiskStatus.CLOSED, nina, "Cartographie complétée", null);

        decision(mobile, "Signature électronique reportée en V2", "COPIL", today.minusDays(14), DecisionStatus.APPROVED, lea);
        decision(portail, "Choix du prestataire de paiement", "Comité client", today.minusDays(30), DecisionStatus.APPROVED, lea);
        decision(audit, "Élargir le diagnostic aux filiales", "Sponsor client", null, DecisionStatus.PROPOSED, thomas);

        reports.save(new StatusReport(portail, lea, today.minusDays(21), Health.AMBER, Trend.STABLE,
                "Démarrage du développement, environnement de paiement en attente.", null, null, null));
        reports.save(new StatusReport(portail, lea, today.minusDays(7), Health.GREEN, Trend.UP,
                "Authentification livrée, intégration du paiement en cours.", "API OAuth2 en recette",
                "Finaliser le paiement, tests de charge", null));
        reports.save(new StatusReport(mobile, hugo, today.minusDays(20), Health.RED, Trend.DOWN,
                "La synchronisation hors-ligne bloque la recette terrain.", null, "Banc de test terminaux",
                "Arbitrage staffing nécessaire"));
        reports.save(new StatusReport(audit, thomas, today.minusDays(3), Health.AMBER, Trend.STABLE,
                "Entretiens terminés, cartographie en cours ; consommation du budget à surveiller.",
                "12 entretiens réalisés", "Ateliers de restitution intermédiaire", "Budget consommé plus vite que prévu"));

        absences.save(new Absence(sarah, today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY),
                today.plusWeeks(1).with(java.time.DayOfWeek.WEDNESDAY), AbsenceKind.VACATION, "Congés"));
        absences.save(new Absence(thomas, today.plusWeeks(2).with(java.time.DayOfWeek.THURSDAY),
                today.plusWeeks(2).with(java.time.DayOfWeek.FRIDAY), AbsenceKind.TRAINING, "Formation TOGAF"));

        // Second locataire : ses données ne doivent jamais être visibles depuis Horizon Conseil
        Organization acme = organizations.save(new Organization("Acme Industries", "acme", Plan.FREE));
        AppUser bob = user(acme, "bob", "Bob Durand", Role.OWNER, "Directeur de projet");
        Team acmeTeam = team(acme, "Équipe principale", null, "#64748b");
        member(acmeTeam, bob, ProjectRole.LEAD);
        Project erp = project(acme, acmeTeam, "ERP", "Migration ERP", ProjectType.DEVELOPMENT, null,
                ProjectStatus.ACTIVE, null, ProjectPriority.P1, Health.GREEN, Visibility.ORGANIZATION, "#64748b", bob,
                today.minusDays(10), today.plusDays(90), 200, "Projet d'une autre organisation.");
        task(erp, new EnumMap<>(TaskStatus.class), "Reprise des données articles", TaskStatus.TODO, Priority.HIGH, bob, 8, today.plusDays(12));

        log.info("Données de démonstration créées : Horizon Conseil (*@ezproject.local) et Acme Industries (bob@acme.local)");
    }

    private void risk(Project project, String title, int probability, int impact, RiskStatus status, AppUser owner,
                      String mitigation, LocalDate due) {
        Risk r = new Risk(project);
        r.update(title, null, probability, impact, status, owner, mitigation, due);
        risks.save(r);
    }

    private void decision(Project project, String title, String by, LocalDate on, DecisionStatus status, AppUser author) {
        Decision d = new Decision(project, author);
        d.update(title, null, on, by, status);
        decisions.save(d);
    }

    private void logTime(Project project, AppUser user, LocalDate until, int workingDays, String days) {
        LocalDate d = until.minusDays(1);
        int logged = 0;
        while (logged < workingDays) {
            if (HolidayCalendar.FR.isWorkingDay(d)) {
                timeEntries.save(new TimeEntry(project, null, user, d, new BigDecimal(days), null));
                logged++;
            }
            d = d.minusDays(1);
        }
    }

    private void allocate(Project project, AppUser user, LocalDate start, LocalDate end, String perWeek) {
        allocations.save(new Allocation(project, user, start, end, new BigDecimal(perWeek)));
    }

    private AppUser user(Organization org, String login, String name, Role role, String jobTitle) {
        String domain = org.getSlug().equals("acme") ? "acme.local" : "ezproject.local";
        return users.save(new AppUser(org, login + "@" + domain, name, hash, role, jobTitle));
    }

    private Team team(Organization org, String name, String description, String color) {
        return teams.save(new Team(org, name, description, color));
    }

    private void member(Team team, AppUser user, ProjectRole role) {
        teamMembers.save(new TeamMembership(team, user, role));
    }

    private Project project(Organization org, Team team, String code, String name, ProjectType type, String client,
                            ProjectStatus status, String reason, ProjectPriority priority, Health health,
                            Visibility visibility, String color, AppUser owner, LocalDate start, LocalDate end,
                            Integer budget, String description) {
        Project p = new Project(org, team, code, name, type);
        p.update(name, description, client, type, priority, health, color, owner, start, end, budget);
        p.assign(team, visibility);
        projects.save(p);
        statusChanges.save(new ProjectStatusChange(p, null, ProjectStatus.PLANNED, "Création du projet", owner));
        if (status == ProjectStatus.ON_HOLD) {
            save(p.changeStatus(ProjectStatus.ACTIVE, null, owner));
        }
        save(p.changeStatus(status, reason, owner));
        return p;
    }

    private void save(ProjectStatusChange change) {
        if (change != null) {
            statusChanges.save(change);
        }
    }

    private Task task(Project project, Map<TaskStatus, Integer> positions, String title, TaskStatus status,
                      Priority priority, AppUser assignee, Integer points, LocalDate due) {
        int position = positions.merge(status, 1, Integer::sum) - 1;
        Task t = new Task(project, title, status, position);
        t.update(title, null, priority, assignee, points, due);
        return tasks.save(t);
    }

    private record PhaseSeed(String name, PhaseKind kind, int days, boolean done) {
    }

    private static PhaseSeed phase(String name, int days, boolean done) {
        return new PhaseSeed(name, PhaseKind.PHASE, days, done);
    }

    private static PhaseSeed milestone(String name) {
        return new PhaseSeed(name, PhaseKind.MILESTONE, 0, false);
    }

    private void retro(Project project, List<PhaseSeed> seeds) {
        for (int i = 0; i < seeds.size(); i++) {
            PhaseSeed s = seeds.get(i);
            RetroPhase ph = new RetroPhase(project, s.name(), s.kind(), s.days(), i);
            ph.update(s.name(), s.kind(), s.days(), s.done());
            phases.save(ph);
        }
    }
}
