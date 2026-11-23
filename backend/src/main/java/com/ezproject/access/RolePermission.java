package com.ezproject.access;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Une case cochée de la matrice rôles / permissions d'une organisation. */
@Entity
@Table(name = "role_permission")
public class RolePermission {

    @EmbeddedId
    private Key id;

    protected RolePermission() {
        // JPA
    }

    public RolePermission(Long organizationId, ProjectRole role, Permission permission) {
        this.id = new Key(organizationId, role, permission);
    }

    public ProjectRole getRole() { return id.role; }
    public Permission getPermission() { return id.permission; }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "organization_id")
        private Long organizationId;

        @Enumerated(EnumType.STRING)
        @Column(name = "role")
        private ProjectRole role;

        @Enumerated(EnumType.STRING)
        @Column(name = "permission")
        private Permission permission;

        protected Key() {
        }

        Key(Long organizationId, ProjectRole role, Permission permission) {
            this.organizationId = organizationId;
            this.role = role;
            this.permission = permission;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.organizationId.equals(organizationId) && k.role == role
                    && k.permission == permission;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(organizationId, role, permission);
        }
    }
}
