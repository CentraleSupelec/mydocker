package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.UserRepository;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves changeset 46 arrived and is armed. A migration that ran is not the same as a constraint
 * that rejects a duplicate, and the whole point of the constraint is the case-insensitive part,
 * which comes from the column being citext rather than from anything in the changeset.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UniqueUserEmailConstraintTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Liquibase liquibase = new Liquibase(
                    "dbschema/master.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
            );
            liquibase.dropAll();
            connection.prepareStatement("CREATE EXTENSION IF NOT EXISTS citext").execute();
            liquibase.update(new Contexts());
        }
    }

    @Test
    void theConstraintExistsUnderTheNameTheHandlerKeysOn() throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT conname FROM pg_constraint WHERE conrelid = 'users'::regclass AND contype = 'u'"
             );
             ResultSet resultSet = statement.executeQuery()) {
            boolean found = false;
            while (resultSet.next()) {
                found = found || "uk_users_email".equals(resultSet.getString("conname"));
            }
            assertTrue(found, "changeset 46 did not create uk_users_email");
        }
    }

    @Test
    void rejectsASecondAccountWithTheSameAddressInADifferentCase() {
        saveUser("short@example.com", "first.last@example.com");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> saveUser("First.Last@example.com", "First.Last@example.com")
        );
        assertEquals(1, userRepository.count());
    }

    @Test
    void leavesNullEmailsAlone() throws Exception {
        // Fourteen rows on CentraleSupelec production and two on Virtual Desktop production still
        // carry a NULL email. Postgres permits any number of NULLs under a unique constraint, and
        // that is the only reason those rows do not block the migration.
        insertLegacyRowWithoutEmail("legacy.one@example.com");
        insertLegacyRowWithoutEmail("legacy.two@example.com");

        assertEquals(2, userRepository.count());
    }

    @Test
    void haltsTheMigrationOnAPlatformThatStillHoldsADuplicate() throws Exception {
        // A green migration cannot tell an armed precondition from an absent one, so the halt is
        // provoked: the constraint is dropped, its changelog row is deleted so Liquibase considers
        // the changeset unapplied, a colliding pair is inserted, and the run is repeated. What must
        // happen is a failure carrying the halt message, because a halted precondition fails
        // startup rather than skipping the migration.
        try (Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("ALTER TABLE users DROP CONSTRAINT uk_users_email").execute();
            connection.prepareStatement(
                    "DELETE FROM databasechangelog WHERE filename LIKE '%46.unique_user_email.yml'"
            ).execute();
        }
        saveUser("short@example.com", "first.last@example.com");
        saveUser("First.Last@example.com", "First.Last@example.com");
        assertEquals(2, userRepository.count());

        try {
            Exception thrown = assertThrows(Exception.class, this::runMigration);

            assertTrue(
                    stackMentions(thrown, "two accounts share an email address"),
                    "the halt message did not reach the failure: " + thrown
            );
            assertEquals(2, userRepository.count(), "the halted run must not have changed any row");
        } finally {
            // The duplicate has to go before this method returns. The next test class builds a new
            // Spring context, and that context runs master.xml through the application's own
            // Liquibase bean before any @BeforeEach can reset the schema, so a colliding pair left
            // behind here fails an unrelated test with a halted migration.
            userRepository.deleteAll();
            runMigration();
        }
    }

    private void runMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Liquibase liquibase = new Liquibase(
                    "dbschema/master.xml",
                    new ClassLoaderResourceAccessor(),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
            );
            liquibase.update(new Contexts());
        }
    }

    private boolean stackMentions(Throwable throwable, String fragment) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(fragment)) {
                return true;
            }
            if (cause.getCause() == cause) {
                return false;
            }
        }
        return false;
    }

    private User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(email);
        user.setLastname(email);
        return userRepository.saveAndFlush(user);
    }

    private void insertLegacyRowWithoutEmail(String username) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (username, name, lastname, enabled) VALUES (?, 'Legacy', 'Account', true)"
             )) {
            statement.setString(1, username);
            statement.execute();
        }
    }
}
