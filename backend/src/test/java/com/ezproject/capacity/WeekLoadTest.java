package com.ezproject.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ezproject.calendar.HolidayCalendar;
import com.ezproject.organization.Organization;
import com.ezproject.organization.Plan;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectType;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;

class WeekLoadTest {

    private final Organization org = new Organization("Org", "org", Plan.TEAM);
    private final AppUser user = new AppUser(org, "a@b.c", "A", "x", Role.MEMBER, null);
    private final Project project = new Project(org, null, "P", "P", ProjectType.DEVELOPMENT);
    private static final LocalDate CHRISTMAS_WEEK = LocalDate.of(2026, 12, 21);

    @Test
    void holidaysAndAbsencesReduceAvailability() {
        var absence = new Absence(user, LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 22), AbsenceKind.VACATION, null);
        var allocation = new Allocation(project, user, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 31), new BigDecimal("3"));

        var load = CapacityService.weekLoad(user, CHRISTMAS_WEEK, HolidayCalendar.FR, List.of(absence), List.of(allocation));

        assertThat(load.capacity()).isEqualByComparingTo("4");   // 25/12 férié
        assertThat(load.absent()).isEqualByComparingTo("2");
        assertThat(load.available()).isEqualByComparingTo("2");
        assertThat(load.allocated()).isEqualByComparingTo("2.4"); // 3 j/sem. x 4 jours ouvrés / 5
        assertThat(load.loadPct()).isEqualTo(120);
    }

    @Test
    void partTimeCapacity() {
        user.changeCapacity(new BigDecimal("4"));
        var load = CapacityService.weekLoad(user, LocalDate.of(2026, 11, 23), HolidayCalendar.FR, List.of(), List.of());
        assertThat(load.capacity()).isEqualByComparingTo("4");
        assertThat(load.loadPct()).isZero();
    }
}
