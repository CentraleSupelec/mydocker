package fr.centralesupelec.thuv.migration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Changeset 51 run for real against the test database, in a schema of its own.
 *
 * Covers the two ways the seed can break a start that has nothing to do with it: parameters
 * missing from the configuration, and parameters that change after the seed has run.
 */
class DefaultClusterSeedMigrationTest {
    private static final String SEED_ID = "1792000000000-2";
    private static final String SCHEMA = "seed_migration_test";
    private static final Map<String, String> SEEDED = Map.of(
            "defaultClusterGoAddress", "10.0.0.5",
            "defaultClusterGoPort", "50001",
            "defaultClusterTerraformStateUrl", "https://bucket.example/tfstate.tf",
            "defaultClusterDeploymentEnabled", "true"
    );

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv().getOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/thuvtest");
        String user = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "thuv");
        String password = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "password");
        connection = DriverManager.getConnection(url, user, password);
        execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        execute("CREATE SCHEMA " + SCHEMA);
        // citext lives in public; keep it on the path.
        execute("SET search_path TO " + SCHEMA + ", public");
    }

    @AfterEach
    void tearDown() throws Exception {
        execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        connection.close();
    }

    @Test
    void aFreshInstallWithoutParametersSeedsNothing() throws Exception {
        migrate(Map.of());

        assertThat(count("clusters")).isZero();
        assertThat(seedRecorded()).isTrue();
    }

    @Test
    void anUpgradeWithoutParametersHaltsAndRecordsNothing() throws Exception {
        migrate(Map.of());
        rewindToSingleGoPlatform();

        assertThatThrownBy(() -> migrate(Map.of()))
                .hasStackTraceContaining("Default cluster seed halted");
        assertThat(count("clusters")).isZero();
        assertThat(seedRecorded()).isFalse();
    }

    @Test
    void anUpgradeWithParametersSeedsAndAssignsEverything() throws Exception {
        migrate(Map.of());
        rewindToSingleGoPlatform();

        migrate(SEEDED);

        assertThat(query("SELECT name || ' ' || go_address || ':' || go_port || ' ' || terraform_state_url || ' ' || deployment_enabled FROM clusters"))
                .isEqualTo("default 10.0.0.5:50001 https://bucket.example/tfstate.tf true");
        assertThat(count("compute_types WHERE cluster_id IS NULL")).isZero();
        assertThat(seedRecorded()).isTrue();
    }

    // The seed values matter once. Changing or removing them afterwards must not stop the start.
    @Test
    void aRestartWithChangedOrRemovedParametersStillValidates() throws Exception {
        migrate(Map.of());
        rewindToSingleGoPlatform();
        migrate(SEEDED);

        migrate(Map.of(
                "defaultClusterGoAddress", "10.9.9.9",
                "defaultClusterGoPort", "50000",
                "defaultClusterTerraformStateUrl", "",
                "defaultClusterDeploymentEnabled", "false"
        ));
        migrate(Map.of());

        assertThat(count("clusters")).isEqualTo(1);
        assertThat(query("SELECT go_address FROM clusters")).isEqualTo("10.0.0.5");
    }

    private void migrate(Map<String, String> parameters) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(SCHEMA);
        Liquibase liquibase = new Liquibase(
                "dbschema/master.xml", new ClassLoaderResourceAccessor(), database
        );
        parameters.forEach(liquibase::setChangeLogParameter);
        liquibase.update("");
    }

    // The state of a platform that ran one Go API: courses, no cluster, seed not yet run.
    private void rewindToSingleGoPlatform() throws Exception {
        execute("DELETE FROM databasechangelog WHERE id = '" + SEED_ID + "'");
        execute("UPDATE compute_types SET cluster_id = NULL");
        execute("DELETE FROM clusters");
        execute("INSERT INTO users (id) VALUES (900001)");
        execute("INSERT INTO courses (id, user_id, status, compute_type_id, command) "
                + "SELECT 900001, 900001, 'OPEN', min(id), 'sleep infinity' FROM compute_types");
    }

    private boolean seedRecorded() throws Exception {
        return count("databasechangelog WHERE id = '" + SEED_ID + "'") == 1;
    }

    private long count(String fromWhere) throws Exception {
        return Long.parseLong(query("SELECT count(*) FROM " + fromWhere));
    }

    private String query(String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getString(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
