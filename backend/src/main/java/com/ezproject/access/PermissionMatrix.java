package com.ezproject.access;

import java.util.Map;
import java.util.Set;

/** Matrice complète : rôle projet -> permissions accordées. */
public record PermissionMatrix(boolean customized, Map<ProjectRole, Set<Permission>> roles) {
}
