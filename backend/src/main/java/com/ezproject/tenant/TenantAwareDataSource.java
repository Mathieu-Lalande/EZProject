package com.ezproject.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ezproject.security.TenantPrincipal;

/**
 * Pose l'organisation de l'utilisateur courant dans la variable de session PostgreSQL {@code app.org_id}
 * à chaque emprunt de connexion : les politiques Row-Level Security (migration vendor/postgresql) filtrent
 * alors toutes les lectures et écritures sur ce locataire. La valeur est réécrite à chaque emprunt, elle ne
 * peut donc pas fuiter d'une requête à l'autre via le pool. Sans utilisateur (connexion, migrations,
 * tâches système), la variable est vide et les politiques laissent l'accès global.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private volatile Boolean postgres;

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withTenant(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withTenant(super.getConnection(username, password));
    }

    private Connection withTenant(Connection connection) throws SQLException {
        if (postgres == null) {
            postgres = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
        }
        if (postgres) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT set_config('app.org_id', ?, false)")) {
                ps.setString(1, currentOrganization());
                ps.execute();
            }
        }
        return connection;
    }

    static String currentOrganization() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantPrincipal p && p.getOrganizationId() != null) {
            return String.valueOf(p.getOrganizationId());
        }
        return "";
    }
}
