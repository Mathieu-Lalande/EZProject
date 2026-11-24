package com.ezproject.time;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.audit.AuditService;
import com.ezproject.calendar.HolidayCalendar;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.common.PageDto;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectRepository;
import com.ezproject.task.Task;
import com.ezproject.task.TaskRepository;
import com.ezproject.time.TimeDtos.EntryDto;
import com.ezproject.time.TimeDtos.EntryRequest;
import com.ezproject.time.TimeDtos.MonthDaysDto;
import com.ezproject.time.TimeDtos.SummaryDto;
import com.ezproject.time.TimeDtos.UserDaysDto;
import com.ezproject.time.TimeDtos.WeekCell;
import com.ezproject.time.TimeDtos.WeekDto;
import com.ezproject.time.TimeDtos.WeekProject;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

/** Saisie des temps passés et suivi de la consommation du budget des projets. */
@Service
@Transactional(readOnly = true)
public class TimeService {

    private static final BigDecimal ONE_DAY = BigDecimal.ONE;
    private static final BigDecimal STEP = new BigDecimal("0.125");

    private final TimeEntryRepository repository;
    private final ProjectAccessService access;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final UserService userService;
    private final AuditService audit;
    private final Clock clock;

    public TimeService(TimeEntryRepository repository, ProjectAccessService access, ProjectRepository projects,
                       TaskRepository tasks, UserService userService, AuditService audit, Clock clock) {
        this.repository = repository;
        this.access = access;
        this.projects = projects;
        this.tasks = tasks;
        this.userService = userService;
        this.audit = audit;
        this.clock = clock;
    }

    public SummaryDto summary(Long projectId) {
        Project project = access.require(projectId, Permission.TIME_VIEW);
        LocalDate today = LocalDate.now(clock);
        List<TimeEntryRepository.DatedDays> dated = repository.datedDays(projectId);
        BigDecimal consumed = dated.stream().map(TimeEntryRepository.DatedDays::getDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        BigDecimal lastFourWeeks = BigDecimal.ZERO;
        for (TimeEntryRepository.DatedDays d : dated) {
            byMonth.merge(YearMonth.from(d.getDate()), d.getDays(), BigDecimal::add);
            if (!d.getDate().isBefore(today.minusDays(28)) && !d.getDate().isAfter(today)) {
                lastFourWeeks = lastFourWeeks.add(d.getDays());
            }
        }
        BigDecimal weeklyBurn = lastFourWeeks.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        Integer budget = project.getBudgetDays();
        BigDecimal remaining = budget == null ? null : BigDecimal.valueOf(budget).subtract(consumed);
        Integer pct = budget == null || budget == 0 ? null
                : consumed.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(budget), 0, RoundingMode.HALF_UP).intValue();
        LocalDate exhaustion = null;
        if (remaining != null && remaining.signum() > 0 && weeklyBurn.signum() > 0) {
            long weeks = remaining.divide(weeklyBurn, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(7)).longValue();
            exhaustion = today.plusDays(weeks);
        }
        return new SummaryDto(budget, consumed, remaining, pct, weeklyBurn, exhaustion,
                remaining != null && remaining.signum() < 0,
                repository.sumByUser(projectId).stream().map(u -> new UserDaysDto(u.getUserId(), u.getName(), u.getDays())).toList(),
                byMonth.entrySet().stream().map(e -> new MonthDaysDto(e.getKey().toString(), e.getValue())).toList());
    }

    public PageDto<EntryDto> entries(Long projectId, int page, int size) {
        Project project = access.require(projectId, Permission.TIME_VIEW);
        AccessContext ctx = access.context();
        boolean manage = ctx.can(project, Permission.TIME_MANAGE);
        Long me = ctx.user().getId();
        return PageDto.from(repository.findByProjectIdOrderByWorkDateDescIdDesc(projectId,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))),
                e -> toDto(e, manage || e.getUser().getId().equals(me)));
    }

    /** Somme des jours consommés par projet (vue portefeuille). */
    public Map<Long, BigDecimal> consumedByProject(List<Long> projectIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (!projectIds.isEmpty()) {
            repository.sumByProject(projectIds).forEach(p -> result.put(p.getProjectId(), p.getDays()));
        }
        return result;
    }

    @Transactional
    public EntryDto log(Long projectId, EntryRequest request) {
        Project project = access.require(projectId, Permission.TIME_LOG, ModuleType.TIME_TRACKING);
        AppUser me = access.context().user();
        AppUser target = me;
        if (request.userId() != null && !request.userId().equals(me.getId())) {
            access.require(project, Permission.TIME_MANAGE);
            target = userService.getInOrganization(request.userId(), project.getOrganization().getId());
            if (!target.isActive()) {
                throw new BusinessRuleException("Ce compte est désactivé");
            }
        }
        validate(target, request.date(), request.days(), null, project.getOrganization().getHolidayCalendar());
        TimeEntry entry = repository.save(new TimeEntry(project, task(request.taskId(), project), target,
                request.date(), request.days(), blankToNull(request.comment())));
        audit.record("TIME_LOGGED", "TIME", entry.getId(),
                target.getDisplayName() + " : " + request.days().stripTrailingZeros().toPlainString() + " j sur "
                        + project.getCode() + " le " + request.date());
        return toDto(entry, true);
    }

    @Transactional
    public EntryDto update(Long entryId, EntryRequest request) {
        TimeEntry entry = editable(entryId);
        Project project = entry.getProject();
        validate(entry.getUser(), request.date(), request.days(), entry.getId(), project.getOrganization().getHolidayCalendar());
        entry.update(task(request.taskId(), project), request.date(), request.days(), blankToNull(request.comment()));
        return toDto(entry, true);
    }

    @Transactional
    public void delete(Long entryId) {
        TimeEntry entry = editable(entryId);
        audit.record("TIME_DELETED", "TIME", entryId, entry.getUser().getDisplayName() + " : suppression de "
                + entry.getDays().stripTrailingZeros().toPlainString() + " j sur " + entry.getProject().getCode());
        repository.delete(entry);
    }

    /** Feuille de temps de la semaine (lundi → dimanche) de l'utilisateur courant. */
    public WeekDto myWeek(LocalDate anyDay) {
        AccessContext ctx = access.context();
        AppUser me = ctx.user();
        LocalDate monday = anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        HolidayCalendar calendar = me.getOrganization().getHolidayCalendar();
        List<LocalDate> days = monday.datesUntil(sunday.plusDays(1)).toList();

        List<WeekProject> loggable = projects.findByOrganizationIdOrderByNameAsc(me.getOrganization().getId()).stream()
                .filter(p -> p.getStatus().isOpen() && p.hasModule(ModuleType.TIME_TRACKING))
                .filter(p -> ctx.can(p, Permission.TIME_LOG))
                .map(p -> new WeekProject(p.getId(), p.getCode(), p.getName(), p.getColor()))
                .toList();
        List<TimeEntry> entries = repository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(me.getId(), monday, sunday);
        Map<String, BigDecimal> grid = new TreeMap<>();
        List<EntryDto> taskEntries = new ArrayList<>();
        for (TimeEntry e : entries) {
            if (e.getTask() == null) {
                grid.merge(e.getProject().getId() + "|" + e.getWorkDate(), e.getDays(), BigDecimal::add);
            } else {
                taskEntries.add(toDto(e, true));
            }
        }
        List<WeekCell> cells = grid.entrySet().stream().map(en -> {
            String[] k = en.getKey().split("\\|");
            return new WeekCell(Long.valueOf(k[0]), LocalDate.parse(k[1]), en.getValue());
        }).toList();
        return new WeekDto(monday, days, days.stream().map(calendar::isWorkingDay).toList(), loggable, cells, taskEntries);
    }

    /**
     * Enregistre la feuille de temps : chaque cellule remplace le temps "hors tâche" de l'utilisateur sur
     * ce projet et ce jour (0 = suppression). Les temps rattachés à une tâche ne sont pas modifiés.
     */
    @Transactional
    public WeekDto saveWeek(TimeDtos.WeekRequest request) {
        AccessContext ctx = access.context();
        AppUser me = ctx.user();
        LocalDate monday = request.weekStart().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        HolidayCalendar calendar = me.getOrganization().getHolidayCalendar();
        Map<Long, Project> cache = new HashMap<>();
        for (WeekCell cell : request.cells()) {
            if (cell.date().isBefore(monday) || cell.date().isAfter(monday.plusDays(6))) {
                throw new BusinessRuleException("Date hors de la semaine : " + cell.date());
            }
            Project project = cache.computeIfAbsent(cell.projectId(),
                    id -> access.require(id, Permission.TIME_LOG, ModuleType.TIME_TRACKING));
            List<TimeEntry> existing = repository.findByUserIdAndProjectIdAndWorkDateAndTaskIsNull(me.getId(),
                    project.getId(), cell.date());
            repository.deleteAll(existing);
            repository.flush();
            if (cell.days().signum() > 0) {
                validate(me, cell.date(), cell.days(), null, calendar);
                repository.save(new TimeEntry(project, null, me, cell.date(), cell.days(), null));
            }
        }
        repository.flush();
        audit.record("TIMESHEET_SAVED", "TIME", null, "Feuille de temps de la semaine du " + monday);
        return myWeek(monday);
    }

    private void validate(AppUser user, LocalDate date, BigDecimal days, Long excludeId, HolidayCalendar calendar) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            throw new BusinessRuleException("Impossible de saisir du temps dans le futur");
        }
        if (date.isBefore(today.minusYears(1))) {
            throw new BusinessRuleException("Saisie limitée aux 12 derniers mois");
        }
        if (days.signum() <= 0 || days.compareTo(ONE_DAY) > 0 || days.remainder(STEP).signum() != 0) {
            throw new BusinessRuleException("Durée invalide : de 0,125 à 1 jour, par pas d'une heure (0,125 j)");
        }
        BigDecimal total = repository.sumForUserOnDate(user.getId(), date, excludeId).add(days);
        if (total.compareTo(ONE_DAY) > 0) {
            throw new BusinessRuleException("Plus d'une journée saisie le " + date + " ("
                    + total.stripTrailingZeros().toPlainString() + " j)");
        }
        if (!calendar.isWorkingDay(date) && days.signum() > 0) {
            // Autorisé (astreinte, week-end travaillé) mais tracé pour le pilotage
            audit.record("TIME_NON_WORKING_DAY", "TIME", null, user.getDisplayName() + " : saisie un jour non ouvré (" + date + ")");
        }
    }

    private TimeEntry editable(Long entryId) {
        TimeEntry entry = repository.findById(entryId).orElseThrow(() -> new NotFoundException("Temps", entryId));
        AccessContext ctx = access.context();
        if (!ctx.canSee(entry.getProject())) {
            throw new NotFoundException("Temps", entryId);
        }
        boolean own = entry.getUser().getId().equals(ctx.user().getId()) && ctx.can(entry.getProject(), Permission.TIME_LOG);
        if (!own && !ctx.can(entry.getProject(), Permission.TIME_MANAGE)) {
            throw new AccessDeniedException("Seuls l'auteur et les responsables peuvent modifier ce temps");
        }
        return entry;
    }

    private Task task(Long taskId, Project project) {
        if (taskId == null) {
            return null;
        }
        Task task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Tâche", taskId));
        if (!task.getProject().getId().equals(project.getId())) {
            throw new BusinessRuleException("La tâche n'appartient pas à ce projet");
        }
        return task;
    }

    private static EntryDto toDto(TimeEntry e, boolean editable) {
        return new EntryDto(e.getId(), e.getWorkDate(), e.getDays(), e.getUser().getId(), e.getUser().getDisplayName(),
                e.getTask() == null ? null : e.getTask().getId(), e.getTask() == null ? null : e.getTask().getTitle(),
                e.getComment(), editable);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
