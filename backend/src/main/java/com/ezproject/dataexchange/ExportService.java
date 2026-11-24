package com.ezproject.dataexchange;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.portfolio.PortfolioDto;
import com.ezproject.portfolio.PortfolioService;
import com.ezproject.project.Project;
import com.ezproject.task.Task;
import com.ezproject.task.TaskRepository;

/** Exports CSV, limités aux données que l'utilisateur a le droit de voir. */
@Service
@Transactional(readOnly = true)
public class ExportService {

    static final String PROJECT_TEMPLATE = CsvExport.BOM + CsvExport.line(List.of("code", "nom", "type", "statut",
            "motif_statut", "priorite", "meteo", "client", "equipe", "responsable", "visibilite", "debut", "echeance",
            "budget_jours", "description"))
            + CsvExport.line(List.of("CRM2027", "Refonte CRM", "Développement", "En cours", "", "P2", "Vert",
            "Client SA", "Delivery Web", "", "Équipe", "06/01/2027", "30/06/2027", "120", "Exemple : à remplacer"));

    static final String TASK_TEMPLATE = CsvExport.BOM + CsvExport.line(List.of("projet", "titre", "statut", "priorite",
            "assigne", "points", "echeance", "description"))
            + CsvExport.line(List.of("CRM2027", "Atelier de cadrage", "À faire", "Haute", "", "3", "15/01/2027", ""));

    private final PortfolioService portfolioService;
    private final TaskRepository taskRepository;
    private final ProjectAccessService access;
    private final Clock clock;

    public ExportService(PortfolioService portfolioService, TaskRepository taskRepository, ProjectAccessService access,
                         Clock clock) {
        this.portfolioService = portfolioService;
        this.taskRepository = taskRepository;
        this.access = access;
        this.clock = clock;
    }

    public String portfolioCsv(Long teamId) {
        List<PortfolioDto.ProjectSummary> projects = portfolioService.summarize(
                portfolioService.visibleProjects(teamId), LocalDate.now(clock));
        StringBuilder sb = new StringBuilder(CsvExport.BOM);
        sb.append(CsvExport.line(List.of("code", "nom", "client", "type", "equipe", "priorite", "statut", "motif_statut",
                "meteo", "responsable", "debut", "echeance", "jours_restants", "avancement_pct", "taches", "taches_terminees",
                "taches_en_retard", "phases_en_retard", "prochain_jalon", "date_jalon", "a_risque", "budget_jours",
                "jours_consommes", "budget_consomme_pct")));
        for (PortfolioDto.ProjectSummary p : projects) {
            sb.append(CsvExport.line(java.util.Arrays.asList(p.code(), p.name(), p.client(), p.type(), p.teamName(),
                    p.priority(), p.status(), p.statusReason(), p.health(), p.ownerName(), p.startDate(), p.endDate(),
                    p.daysRemaining(), p.progress(), p.tasksTotal(), p.tasksByStatus().get(com.ezproject.task.TaskStatus.DONE),
                    p.overdueTasks(), p.latePhases(), p.nextMilestone() == null ? null : p.nextMilestone().name(),
                    p.nextMilestone() == null ? null : p.nextMilestone().date(), p.atRisk() ? "oui" : "non",
                    p.budgetDays(), p.consumedDays(), p.budgetPct())));
        }
        return sb.toString();
    }

    public String tasksCsv(Long teamId) {
        AccessContext ctx = access.context();
        List<Long> ids = portfolioService.visibleProjects(teamId).stream()
                .filter(p -> ctx.can(p, Permission.TASK_VIEW)).map(Project::getId).toList();
        StringBuilder sb = new StringBuilder(CsvExport.BOM);
        sb.append(CsvExport.line(List.of("projet", "titre", "statut", "priorite", "assigne", "points", "echeance",
                "description")));
        if (!ids.isEmpty()) {
            for (Task t : taskRepository.findByProjectIdInOrderByProjectIdAscStatusAscPositionAsc(ids)) {
                sb.append(CsvExport.line(java.util.Arrays.asList(t.getProject().getCode(), t.getTitle(), t.getStatus(),
                        t.getPriority(), t.getAssignee() == null ? null : t.getAssignee().getEmail(),
                        t.getStoryPoints(), t.getDueDate(), t.getDescription())));
            }
        }
        return sb.toString();
    }
}
