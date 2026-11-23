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

import com.ezproject.poker.PokerRound;
import com.ezproject.poker.PokerRoundRepository;
import com.ezproject.project.Health;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectRepository;
import com.ezproject.project.ProjectStatus;
import com.ezproject.project.ProjectType;
import com.ezproject.retroplanning.RetroPhase;
import com.ezproject.retroplanning.RetroPhaseRepository;
import com.ezproject.retroplanning.RetroPlanningService;
import com.ezproject.task.Priority;
import com.ezproject.task.Task;
import com.ezproject.task.TaskRepository;
import com.ezproject.task.TaskStatus;
import com.ezproject.todo.TodoItem;
import com.ezproject.todo.TodoRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;
import com.ezproject.user.Role;

/** Jeu de données de démonstration, activé uniquement via app.seed.enabled=true (profil dev). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppUserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TodoRepository todos;
    private final RetroPhaseRepository phases;
    private final RetroPlanningService retroPlanning;
    private final PokerRoundRepository poker;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String demoPassword;

    DemoDataSeeder(AppUserRepository users, ProjectRepository projects, TaskRepository tasks, TodoRepository todos,
                   RetroPhaseRepository phases, RetroPlanningService retroPlanning, PokerRoundRepository poker,
                   PasswordEncoder encoder, Clock clock, @Value("${app.seed.demo-password}") String demoPassword) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.todos = todos;
        this.phases = phases;
        this.retroPlanning = retroPlanning;
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
        LocalDate today = LocalDate.now(clock);
        String hash = encoder.encode(demoPassword);
        AppUser camille = users.save(new AppUser("camille@ezproject.local", "Camille Martin", hash, Role.ADMIN, "COO"));
        AppUser lea = users.save(new AppUser("lea@ezproject.local", "Léa Dubois", hash, Role.MANAGER, "PMO"));
        AppUser hugo = users.save(new AppUser("hugo@ezproject.local", "Hugo Bernard", hash, Role.MEMBER, "Développeur Java"));
        AppUser sarah = users.save(new AppUser("sarah@ezproject.local", "Sarah Petit", hash, Role.MEMBER, "Développeuse React"));
        AppUser thomas = users.save(new AppUser("thomas@ezproject.local", "Thomas Roux", hash, Role.MEMBER, "Consultant SI"));

        Project portail = project("PORTAIL", "Portail client", ProjectType.DEVELOPMENT, "Banque Horizon",
                ProjectStatus.ACTIVE, Health.GREEN, "#6366f1", lea, today.minusDays(40), today.plusDays(45),
                "Refonte du portail client : authentification, tableau de bord et paiements.");
        Project audit = project("AUDITSI", "Audit SI groupe", ProjectType.CONSULTING, "Mutuelle Atlas",
                ProjectStatus.ACTIVE, Health.AMBER, "#f59e0b", thomas, today.minusDays(12), today.plusDays(28),
                "Diagnostic du système d'information et feuille de route à 3 ans.");
        Project mobile = project("MOBILE", "App mobile terrain", ProjectType.DEVELOPMENT, "Énergies du Sud",
                ProjectStatus.ACTIVE, Health.RED, "#ef4444", lea, today.minusDays(60), today.plusDays(18),
                "Application de saisie hors-ligne pour les techniciens.");
        Project rh = project("ONBOARD", "Onboarding RH", ProjectType.INTERNAL, null,
                ProjectStatus.PLANNED, Health.GREEN, "#10b981", camille, today.plusDays(7), today.plusDays(50),
                "Digitaliser le parcours d'arrivée des nouveaux collaborateurs.");

        Map<TaskStatus, Integer> pos = new EnumMap<>(TaskStatus.class);
        task(portail, pos, "Maquettes du tableau de bord", TaskStatus.DONE, Priority.HIGH, sarah, 5, today.minusDays(20));
        task(portail, pos, "API d'authentification OAuth2", TaskStatus.DONE, Priority.CRITICAL, hugo, 8, today.minusDays(10));
        Task paiement = task(portail, pos, "Intégration prestataire de paiement", TaskStatus.IN_PROGRESS, Priority.HIGH, hugo, null, today.plusDays(6));
        task(portail, pos, "Écran historique des opérations", TaskStatus.IN_PROGRESS, Priority.MEDIUM, sarah, 5, today.plusDays(9));
        task(portail, pos, "Tests de charge", TaskStatus.TODO, Priority.MEDIUM, hugo, 3, today.plusDays(20));
        task(portail, pos, "Accessibilité RGAA", TaskStatus.REVIEW, Priority.MEDIUM, sarah, 3, today.plusDays(2));
        task(portail, pos, "Export PDF des relevés", TaskStatus.BACKLOG, Priority.LOW, null, null, null);
        task(portail, pos, "Notifications push", TaskStatus.BACKLOG, Priority.LOW, null, null, null);

        pos.clear();
        task(audit, pos, "Entretiens DSI et métiers", TaskStatus.DONE, Priority.HIGH, thomas, null, today.minusDays(3));
        task(audit, pos, "Cartographie applicative", TaskStatus.IN_PROGRESS, Priority.HIGH, thomas, null, today.minusDays(1));
        task(audit, pos, "Analyse des coûts d'infrastructure", TaskStatus.TODO, Priority.MEDIUM, thomas, null, today.plusDays(8));
        task(audit, pos, "Préparer support COPIL", TaskStatus.TODO, Priority.HIGH, lea, null, today.plusDays(25));

        pos.clear();
        task(mobile, pos, "Synchronisation hors-ligne", TaskStatus.IN_PROGRESS, Priority.CRITICAL, hugo, 13, today.minusDays(5));
        task(mobile, pos, "Signature électronique", TaskStatus.TODO, Priority.HIGH, sarah, 8, today.minusDays(2));
        task(mobile, pos, "Publication stores", TaskStatus.TODO, Priority.MEDIUM, null, 3, today.plusDays(15));
        task(mobile, pos, "Formulaire d'intervention", TaskStatus.DONE, Priority.HIGH, sarah, 5, today.minusDays(15));

        pos.clear();
        task(rh, pos, "Recenser les étapes actuelles", TaskStatus.TODO, Priority.MEDIUM, camille, null, today.plusDays(10));

        retroPlanning.applyTemplate(portail.getId(), ProjectType.DEVELOPMENT);
        retroPlanning.applyTemplate(audit.getId(), ProjectType.CONSULTING);
        retroPlanning.applyTemplate(mobile.getId(), ProjectType.DEVELOPMENT);
        markFirstPhasesDone(portail, 2);
        markFirstPhasesDone(audit, 1);
        markFirstPhasesDone(mobile, 1);

        todos.save(new TodoItem(camille, null, "Relire le reporting mensuel du portefeuille", today.plusDays(1)));
        todos.save(new TodoItem(camille, null, "Arbitrer le staffing de l'app mobile", today));
        todos.save(new TodoItem(lea, portail, "Envoyer le compte-rendu du comité de suivi", today.plusDays(2)));
        todos.save(new TodoItem(thomas, audit, "Réserver la salle pour les ateliers", today.plusDays(4)));

        poker.save(new PokerRound(portail, paiement, paiement.getTitle()));

        log.info("Données de démonstration créées (comptes *@ezproject.local)");
    }

    private Project project(String code, String name, ProjectType type, String client, ProjectStatus status,
                            Health health, String color, AppUser owner, LocalDate start, LocalDate end,
                            String description) {
        Project p = new Project(code, name, type);
        p.update(name, description, client, type, status, health, color, owner, start, end, null);
        return projects.save(p);
    }

    private Task task(Project project, Map<TaskStatus, Integer> positions, String title, TaskStatus status,
                      Priority priority, AppUser assignee, Integer points, LocalDate due) {
        int position = positions.merge(status, 1, Integer::sum) - 1;
        Task t = new Task(project, title, status, position);
        t.update(title, null, priority, assignee, points, due);
        return tasks.save(t);
    }

    private void markFirstPhasesDone(Project project, int count) {
        List<RetroPhase> list = phases.findByProjectIdOrderBySortOrderAsc(project.getId());
        for (int i = 0; i < Math.min(count, list.size()); i++) {
            RetroPhase ph = list.get(i);
            ph.update(ph.getName(), ph.getKind(), ph.getDurationDays(), true);
        }
    }
}
