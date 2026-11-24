package com.ezproject.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Utilisateur authentifié portant son organisation (locataire), utilisée notamment par le RLS PostgreSQL. */
public class TenantPrincipal extends User {

    private final Long organizationId;

    public TenantPrincipal(String username, String password, boolean enabled,
                           Collection<? extends GrantedAuthority> authorities, Long organizationId) {
        super(username, password, enabled, true, true, true, authorities);
        this.organizationId = organizationId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
