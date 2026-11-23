package com.ezproject.access;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permissions fines sur les éléments d'un projet. Chaque rôle projet reçoit un ensemble de permissions,
 * personnalisable par organisation (matrice rôles / permissions).
 */
public enum Permission {
    PROJECT_EDIT,
    PROJECT_MODULES,
    PROJECT_MEMBERS,
    PROJECT_DELETE,
    TASK_VIEW,
    TASK_CREATE,
    TASK_EDIT,
    TASK_DELETE,
    TODO_VIEW,
    TODO_EDIT,
    RETRO_VIEW,
    RETRO_EDIT,
    POKER_VIEW,
    POKER_VOTE,
    POKER_FACILITATE,
    /** Voir les temps passés et la consommation du budget. */
    TIME_VIEW,
    /** Saisir ses propres temps sur le projet. */
    TIME_LOG,
    /** Gérer les temps des autres et le staffing (affectations). */
    TIME_MANAGE,
    RISK_VIEW,
    RISK_EDIT,
    /** Publier la météo / le rapport d'avancement du projet. */
    REPORT_EDIT,
    /** Commenter les tâches. */
    TASK_COMMENT;

    /** Matrice par défaut, appliquée tant que l'organisation ne l'a pas personnalisée. */
    public static Set<Permission> defaultsFor(ProjectRole role) {
        return switch (role) {
            // La suppression d'un projet reste réservée aux administrateurs par défaut
            case LEAD -> EnumSet.complementOf(EnumSet.of(PROJECT_DELETE));
            case CONTRIBUTOR -> EnumSet.of(TASK_VIEW, TASK_CREATE, TASK_EDIT, TODO_VIEW, TODO_EDIT, RETRO_VIEW,
                    POKER_VIEW, POKER_VOTE, POKER_FACILITATE, TIME_VIEW, TIME_LOG, RISK_VIEW, RISK_EDIT, TASK_COMMENT);
            // Les lecteurs (souvent des clients) ne voient ni les temps ni le budget par défaut
            case VIEWER -> EnumSet.of(TASK_VIEW, TODO_VIEW, RETRO_VIEW, POKER_VIEW, RISK_VIEW);
        };
    }
}
