package com.ezproject.capacity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.access.ProjectRole;
import com.ezproject.audit.AuditService;
import com.ezproject.calendar.HolidayCalendar;
import com.ezproject.capacity.CapacityDtos.AbsenceDto;
import com.ezproject.capacity.CapacityDtos.AbsenceRequest;
import com.ezproject.capacity.CapacityDtos.AllocationDto;
import com.ezproject.capacity.CapacityDtos.AllocationRequest;
import com.ezproject.capacity.CapacityDtos.CapacityDto;
import com.ezproject.capacity.CapacityDtos.PersonCapacity;
import com.ezproject.capacity.CapacityDtos.WeekLoad;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.Project;
import com.ezproject.security.CurrentUser;
import com.ezproject.team.TeamMembership;
import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;
import com.ezproject.user.UserService;

/**
 * Capacité des équipes : disponibilité (temps de travail, jours fériés, absences) comparée aux affectations
 * (staffing) semaine par semaine.
 */
@Service
@Transactional(readOnly = true)
public class CapacityService {

    private static final BigDecimal FIVE = BigDecimal.valueOf(5);

    private final AbsenceRepository absences;
    private final AllocationRepository allocations;
    private final AppUserRepository users;
    private final TeamMembershipRepository memberships;
    private final UserService userService;
    private final ProjectAccessService access;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public CapacityService(AbsenceRepository absences, AllocationRepository allocations, AppUserRepository users,
                           TeamMembershipRepository memberships, UserService userService, ProjectAccessService access,
                           CurrentUser currentUser, AuditService audit) {
        this.absences = absences;
        this.allocations = allocations;
        this.users = users;
        this.memberships = memberships;
        this.userService = userService;
        this.access = access;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- Vue capacité

    /**
     * Charge / capacité sur {@code weekCount} semaines à partir de la semaine de {@code from}.
     * Périmètre : toute l'organisation pour les managers, les membres des équipes dont on est responsable,
     * sinon uniquement soi-même.
     */
    public CapacityDto capacity(LocalDate from, int weekCount, Long teamId) {
        AppUser me = currentUser.get();
        int count = Math.min(Math.max(1, weekCount), 26);
        LocalDate firstMonday = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastSunday = firstMonday.plusWeeks(count).minusDays(1);
        List<LocalDate> weeks = firstMonday.datesUntil(lastSunday.plusDays(1), java.time.Period.ofWeeks(1)).toList();
        HolidayCalendar calendar = me.getOrganization().getHolidayCalendar();

        List<AppUser> people = visiblePeople(me, teamId);
        List<Long> ids = people.stream().map(AppUser::getId).toList();
        if (ids.isEmpty()) {
            return new CapacityDto(weeks, List.of());
        }
        Map<Long, List<Absence>> absencesByUser = absences.overlapping(ids, firstMonday, lastSunday).stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()));
        Map<Long, List<Allocation>> allocationsByUser = allocations.overlapping(ids, firstMonday, lastSunday).stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()));
        AccessContext ctx = access.context();

        List<PersonCapacity> result = people.stream().map(u -> {
            List<Absence> abs = absencesByUser.getOrDefault(u.getId(), List.of());
            List<Allocation> alloc = allocationsByUser.getOrDefault(u.getId(), List.of());
            List<WeekLoad> loads = weeks.stream().map(w -> weekLoad(u, w, calendar, abs, alloc)).toList();
            List<AllocationDto> visibleAllocations = alloc.stream()
                    .filter(a -> ctx.canSee(a.getProject()))
                    .map(CapacityService::toDto).toList();
            return new PersonCapacity(u.getId(), u.getDisplayName(), u.getJobTitle(), u.getCapacityDaysPerWeek(),
                    loads, visibleAllocations);
        }).toList();
        return new CapacityDto(weeks, result);
    }

    static WeekLoad weekLoad(AppUser user, LocalDate monday, HolidayCalendar calendar, List<Absence> abs,
                             List<Allocation> alloc) {
        LocalDate sunday = monday.plusDays(6);
        BigDecimal daily = user.getCapacityDaysPerWeek().divide(FIVE, 4, RoundingMode.HALF_UP);
        int workingDays = calendar.workingDaysBetween(monday, sunday);
        BigDecimal capacity = daily.multiply(BigDecimal.valueOf(workingDays));
        int absentDays = 0;
        for (Absence a : abs) {
            LocalDate start = max(a.getStartDate(), monday);
            LocalDate end = min(a.getEndDate(), sunday);
            if (!end.isBefore(start)) {
                absentDays += calendar.workingDaysBetween(start, end);
            }
        }
        BigDecimal absent = daily.multiply(BigDecimal.valueOf(Math.min(absentDays, workingDays)));
        BigDecimal available = capacity.subtract(absent).max(BigDecimal.ZERO);
        BigDecimal allocated = BigDecimal.ZERO;
        for (Allocation a : alloc) {
            LocalDate start = max(a.getStartDate(), monday);
            LocalDate end = min(a.getEndDate(), sunday);
            if (!end.isBefore(start)) {
                allocated = allocated.add(a.getDaysPerWeek()
                        .multiply(BigDecimal.valueOf(calendar.workingDaysBetween(start, end))).divide(FIVE, 4, RoundingMode.HALF_UP));
            }
        }
        Integer pct = available.signum() == 0
                ? (allocated.signum() > 0 ? 999 : null)
                : allocated.multiply(BigDecimal.valueOf(100)).divide(available, 0, RoundingMode.HALF_UP).intValue();
        return new WeekLoad(monday, scale(capacity), scale(absent), scale(available), scale(allocated), pct);
    }

    private List<AppUser> visiblePeople(AppUser me, Long teamId) {
        Long orgId = me.getOrganization().getId();
        List<AppUser> all = users.findByOrganizationIdAndDeletedAtIsNullOrderByDisplayNameAsc(orgId).stream()
                .filter(AppUser::isActive)
                // Les invités (clients, partenaires) ne font pas partie de la capacité des équipes
                .filter(u -> u.getRole() != com.ezproject.user.Role.GUEST)
                .toList();
        Set<Long> teamFilter = teamId == null ? null : memberships.findByTeamIdIn(List.of(teamId)).stream()
                .map(m -> m.getUser().getId()).collect(Collectors.toSet());
        Set<Long> allowed;
        if (me.getRole().seesWholePortfolio()) {
            allowed = all.stream().map(AppUser::getId).collect(Collectors.toSet());
        } else {
            List<Long> ledTeams = memberships.findByUserId(me.getId()).stream()
                    .filter(m -> m.getRole() == ProjectRole.LEAD).map(m -> m.getTeam().getId()).toList();
            allowed = ledTeams.isEmpty() ? new java.util.HashSet<>()
                    : memberships.findByTeamIdIn(ledTeams).stream().map(TeamMembership::getUser).map(AppUser::getId)
                            .collect(Collectors.toSet());
            allowed.add(me.getId());
        }
        return all.stream()
                .filter(u -> allowed.contains(u.getId()))
                .filter(u -> teamFilter == null || teamFilter.contains(u.getId()))
                .toList();
    }

    // ---------------------------------------------------------------- Absences

    public List<AbsenceDto> absencesInRange(LocalDate from, LocalDate to) {
        AppUser me = currentUser.get();
        List<Long> ids = visiblePeople(me, null).stream().map(AppUser::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return absences.overlapping(ids, from, to).stream().map(a -> toDto(a, me)).toList();
    }

    public List<AbsenceDto> myAbsences() {
        AppUser me = currentUser.get();
        return absences.findByUserIdOrderByStartDateDesc(me.getId()).stream().map(a -> toDto(a, me)).toList();
    }

    @Transactional
    public AbsenceDto createAbsence(AbsenceRequest request) {
        AppUser me = currentUser.get();
        AppUser target = me;
        if (request.userId() != null && !request.userId().equals(me.getId())) {
            requireManager(me);
            target = userService.getInOrganization(request.userId(), me.getOrganization().getId());
        }
        if (request.endDate().isAfter(request.startDate().plusYears(1))) {
            throw new BusinessRuleException("Une absence ne peut pas dépasser un an");
        }
        Absence saved = absences.save(new Absence(target, request.startDate(), request.endDate(), request.kind(),
                request.comment() == null || request.comment().isBlank() ? null : request.comment().trim()));
        // Le motif n'est pas journalisé (donnée potentiellement sensible)
        audit.record("ABSENCE_CREATED", "USER", target.getId(), "Absence de " + target.getDisplayName() + " du "
                + request.startDate() + " au " + request.endDate());
        return toDto(saved, me);
    }

    @Transactional
    public void deleteAbsence(Long id) {
        AppUser me = currentUser.get();
        Absence absence = absences.findById(id).orElseThrow(() -> new NotFoundException("Absence", id));
        if (!absence.getUser().belongsTo(me.getOrganization())) {
            throw new NotFoundException("Absence", id);
        }
        if (!absence.getUser().getId().equals(me.getId())) {
            requireManager(me);
        }
        absences.delete(absence);
    }

    private AbsenceDto toDto(Absence a, AppUser viewer) {
        boolean own = a.getUser().getId().equals(viewer.getId());
        boolean details = own || viewer.getRole().isAdmin();
        return new AbsenceDto(a.getId(), a.getUser().getId(), a.getUser().getDisplayName(), a.getStartDate(),
                a.getEndDate(), details ? a.getKind() : null, details ? a.getComment() : null,
                own || viewer.getRole().seesWholePortfolio());
    }

    // ---------------------------------------------------------------- Affectations (staffing)

    public List<AllocationDto> projectAllocations(Long projectId) {
        access.require(projectId, Permission.TIME_VIEW);
        return allocations.findByProjectIdOrderByStartDateAsc(projectId).stream().map(CapacityService::toDto).toList();
    }

    @Transactional
    public AllocationDto allocate(Long projectId, AllocationRequest request) {
        Project project = access.require(projectId, Permission.TIME_MANAGE);
        AppUser user = userService.getInOrganization(request.userId(), project.getOrganization().getId());
        if (!user.isActive()) {
            throw new BusinessRuleException("Ce compte est désactivé");
        }
        Allocation saved = allocations.save(new Allocation(project, user, request.startDate(), request.endDate(),
                request.daysPerWeek()));
        audit.record("ALLOCATION_CREATED", "PROJECT", projectId, user.getDisplayName() + " affecté(e) "
                + request.daysPerWeek().stripTrailingZeros().toPlainString() + " j/sem. sur " + project.getCode());
        return toDto(saved);
    }

    @Transactional
    public AllocationDto updateAllocation(Long id, AllocationRequest request) {
        Allocation allocation = manageableAllocation(id);
        allocation.update(request.startDate(), request.endDate(), request.daysPerWeek());
        return toDto(allocation);
    }

    @Transactional
    public void deleteAllocation(Long id) {
        Allocation allocation = manageableAllocation(id);
        audit.record("ALLOCATION_DELETED", "PROJECT", allocation.getProject().getId(),
                allocation.getUser().getDisplayName() + " retiré(e) du staffing de " + allocation.getProject().getCode());
        allocations.delete(allocation);
    }

    private Allocation manageableAllocation(Long id) {
        Allocation allocation = allocations.findById(id).orElseThrow(() -> new NotFoundException("Affectation", id));
        access.require(allocation.getProject(), Permission.TIME_MANAGE);
        return allocation;
    }

    // ---------------------------------------------------------------- Temps de travail contractuel

    @Transactional
    public void changeCapacity(Long userId, BigDecimal daysPerWeek) {
        AppUser me = currentUser.get();
        requireManager(me);
        if (daysPerWeek.compareTo(BigDecimal.ZERO) < 0 || daysPerWeek.compareTo(FIVE) > 0) {
            throw new BusinessRuleException("Capacité attendue entre 0 et 5 jours par semaine");
        }
        AppUser target = userService.getInOrganization(userId, me.getOrganization().getId());
        target.changeCapacity(daysPerWeek);
        audit.record("CAPACITY_CHANGED", "USER", userId, target.getDisplayName() + " : "
                + daysPerWeek.stripTrailingZeros().toPlainString() + " j/semaine");
    }

    @Transactional
    public HolidayCalendar changeHolidayCalendar(HolidayCalendar calendar) {
        AppUser me = currentUser.get();
        if (!me.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
        me.getOrganization().changeHolidayCalendar(calendar);
        audit.record("HOLIDAY_CALENDAR", "ORGANIZATION", me.getOrganization().getId(), "Calendrier : " + calendar);
        return calendar;
    }

    private static void requireManager(AppUser me) {
        if (!me.getRole().seesWholePortfolio()) {
            throw new AccessDeniedException("Réservé aux managers et administrateurs");
        }
    }

    static AllocationDto toDto(Allocation a) {
        Project p = a.getProject();
        return new AllocationDto(a.getId(), p.getId(), p.getCode(), p.getName(), p.getColor(), a.getUser().getId(),
                a.getUser().getDisplayName(), a.getStartDate(), a.getEndDate(), a.getDaysPerWeek());
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** Pour les tests : liste des semaines calculées. */
    static List<LocalDate> mondays(LocalDate from, int count) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate monday = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < count; i++) {
            result.add(monday.plusWeeks(i));
        }
        return result;
    }
}
