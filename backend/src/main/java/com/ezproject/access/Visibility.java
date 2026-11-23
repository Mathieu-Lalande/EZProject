package com.ezproject.access;

/**
 * ORGANIZATION : tout membre de l'organisation peut consulter le projet (lecture).
 * TEAM : réservé aux membres de l'équipe propriétaire et aux membres explicites.
 * RESTRICTED : confidentiel, uniquement les membres explicites du projet.
 * Dans tous les cas, administrateurs et managers (vision portefeuille) y ont accès.
 */
public enum Visibility {
    ORGANIZATION, TEAM, RESTRICTED
}
