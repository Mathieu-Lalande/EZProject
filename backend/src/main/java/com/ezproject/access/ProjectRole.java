package com.ezproject.access;

/** Rôle d'un utilisateur sur un projet (hérité de son équipe ou attribué explicitement). */
public enum ProjectRole {
    VIEWER, CONTRIBUTOR, LEAD;

    public static ProjectRole max(ProjectRole a, ProjectRole b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
