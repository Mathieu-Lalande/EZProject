package com.ezproject.organization;

public record OrganizationDto(Long id, String name, String slug, Plan plan, Usage usage,
                              com.ezproject.calendar.HolidayCalendar holidayCalendar) {

    public record Usage(long projects, int maxProjects, long users, int maxUsers, boolean customPermissions) {
    }
}
