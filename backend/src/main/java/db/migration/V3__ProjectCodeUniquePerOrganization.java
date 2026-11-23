package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * V3 : le code projet devient unique par organisation (et non plus globalement).
 * <p>
 * Migration Java car la contrainte UNIQUE de V1 n'a pas été nommée : son nom est généré par le SGBD
 * (project_code_key sous PostgreSQL, CONSTRAINT_xx sous H2). On le retrouve via information_schema,
 * standard et disponible sur les deux bases.
 */
public class V3__ProjectCodeUniquePerOrganization extends BaseJavaMigration {

    private static final String FIND_CONSTRAINTS = """
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
            WHERE LOWER(tc.table_name) = 'project'
              AND tc.constraint_type = 'UNIQUE'
              AND LOWER(kcu.column_name) = 'code'
            """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(FIND_CONSTRAINTS); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        try (Statement st = connection.createStatement()) {
            for (String name : names) {
                // Nom issu du catalogue système (pas d'entrée utilisateur) ; guillemets doublés par précaution
                st.execute("ALTER TABLE project DROP CONSTRAINT \"" + name.replace("\"", "\"\"") + "\"");
            }
            st.execute("ALTER TABLE project ADD CONSTRAINT uk_project_org_code UNIQUE (organization_id, code)");
        }
    }
}
