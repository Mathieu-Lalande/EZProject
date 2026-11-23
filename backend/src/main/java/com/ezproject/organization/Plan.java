package com.ezproject.organization;

/**
 * Offres SaaS et leurs quotas. Un quota à -1 signifie "illimité".
 * Les prix et la facturation ne sont pas gérés ici : ce sont des limites fonctionnelles.
 */
public enum Plan {
    FREE(3, 5, false),
    TEAM(50, 50, true),
    ENTERPRISE(-1, -1, true);

    private final int maxProjects;
    private final int maxUsers;
    private final boolean customPermissions;

    Plan(int maxProjects, int maxUsers, boolean customPermissions) {
        this.maxProjects = maxProjects;
        this.maxUsers = maxUsers;
        this.customPermissions = customPermissions;
    }

    public int maxProjects() { return maxProjects; }
    public int maxUsers() { return maxUsers; }
    public boolean allowsCustomPermissions() { return customPermissions; }

    public boolean allowsMoreProjects(long current) {
        return maxProjects < 0 || current < maxProjects;
    }

    public boolean allowsMoreUsers(long current) {
        return maxUsers < 0 || current < maxUsers;
    }
}
