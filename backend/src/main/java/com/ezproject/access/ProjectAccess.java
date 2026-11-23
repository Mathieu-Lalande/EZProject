package com.ezproject.access;

import java.util.Set;

/** Droits effectifs de l'utilisateur courant sur un projet. */
public record ProjectAccess(ProjectRole role, boolean admin, Set<Permission> permissions) {

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }
}
