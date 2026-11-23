package com.ezproject.user;

/**
 * Rôle d'un utilisateur au niveau de l'organisation.
 * <ul>
 *   <li>OWNER : propriétaire du compte SaaS (offre, suppression), tous les droits.</li>
 *   <li>ADMIN : administre utilisateurs, équipes et permissions, tous les droits projet.</li>
 *   <li>MANAGER : pilotage portefeuille (COO / PMO), responsable sur tous les projets.</li>
 *   <li>MEMBER : accède aux projets de ses équipes, selon son rôle d'équipe.</li>
 *   <li>GUEST : invité externe (client, sous-traitant), uniquement les projets où il est explicitement ajouté.</li>
 * </ul>
 */
public enum Role {
    OWNER, ADMIN, MANAGER, MEMBER, GUEST;

    public boolean isAdmin() {
        return this == OWNER || this == ADMIN;
    }

    /** Vision portefeuille complète de l'organisation. */
    public boolean seesWholePortfolio() {
        return this == OWNER || this == ADMIN || this == MANAGER;
    }
}
