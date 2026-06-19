package fr.centralesupelec.thuv.security;

import static org.junit.jupiter.api.Assertions.*;

import fr.centralesupelec.thuv.exception.UserUpsertException;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MyUserDetailsServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(MyUserDetailsServiceTest.class);

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Liquibase liquibase = new Liquibase("dbschema/master.xml", new ClassLoaderResourceAccessor(), DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection)));
            liquibase.dropAll();
            connection.prepareStatement("CREATE EXTENSION IF NOT EXISTS citext").execute();
            liquibase.update(new Contexts());
        }
        logger.info("Starting test: {}", testInfo.getDisplayName());
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        logger.info("Ending test: {}", testInfo.getDisplayName());
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private MyUserDetailsService myUserDetailsService;

    private static final String usernameCs = "student-id@example.com";
    private static final String emailCs = "first.last@example.com";
    private static final String oldEmailUps = "old.email@example.net";
    private static final String newStudentEmailUps = "student.email@example.org";

    protected User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(email);
        user.setLastname(email);
        return userRepository.saveAndFlush(user);
    }

    private void saveUserRole() {
        Role role = new Role();
        role.setName("ROLE_USER");
        roleRepository.saveAndFlush(role);
    }

    @Test
    void upsertUser_duplicateEmailDoesNotFallbackToInsert() {
        User oidcUser = this.saveUser("short@example.com", "first.last@example.com");
        this.saveUser("First.Last@example.com", "First.Last@example.com");

        assertThrows(
                UserUpsertException.class,
                () -> myUserDetailsService.upsertUser(
                        "short@example.com",
                        "first.last@example.com",
                        "First",
                        "Last"
                )
        );
        assertEquals(2, userRepository.count());
        assertEquals(oidcUser.getId(), userRepository.findByUsername("short@example.com").get().getId());
    }

    @Test
    void upsertUser_disabledSiblingDoesNotBlockResolvedEnabledUser() {
        saveUserRole();
        User enabledUser = this.saveUser("short@example.com", "first.last@example.com");
        User disabledSibling = this.saveUser("First.Last@example.com", "old.email@example.com");
        disabledSibling.setEnabled(false);
        userRepository.saveAndFlush(disabledSibling);

        User result = myUserDetailsService.upsertUser(
                "short@example.com",
                "first.last@example.com",
                "First",
                "Last"
        );

        assertEquals(enabledUser.getId(), result.getId());
        assertFalse(userRepository.findById(disabledSibling.getId()).get().getEnabled());
        assertEquals(2, userRepository.count());
    }

    @Test
    void upsertUser_disabledUserRejected() {
        saveUserRole();
        User banned = this.saveUser("short@example.com", "first.last@example.com");
        banned.setEnabled(false);
        userRepository.saveAndFlush(banned);

        assertThrows(
                DisabledException.class,
                () -> myUserDetailsService.upsertUser(
                        "short@example.com",
                        "first.last@example.com",
                        "First",
                        "Last"
                )
        );
        assertEquals(1, userRepository.count());
    }

    @Test
    void upsertUser_nullUsernameProviderUsesEmailAsUsername() {
        // CAS/LTI provide a null username; email must become the username.
        saveUserRole();
        User user = myUserDetailsService.upsertUser(null, "first.last@example.com", "First", "Last");

        assertEquals("first.last@example.com", user.getUsername());
        assertEquals("first.last@example.com", user.getEmail());
        assertEquals(1, userRepository.count());
    }

    @Test
    void upsertUser_nullUsernameProviderDoesNotOverwriteExistingUsername() {
        // CAS/LTI login resolving an existing OIDC-style row (username != email) must
        // NOT rewrite username to email.
        saveUserRole();
        User existing = this.saveUser("short@example.com", "first.last@example.com");

        User result = myUserDetailsService.upsertUser(null, "first.last@example.com", "First", "Last");

        assertEquals(existing.getId(), result.getId());
        assertEquals("short@example.com", result.getUsername());
        assertEquals("first.last@example.com", result.getEmail());
        assertEquals(1, userRepository.count());
    }

    @Test
    void findOrCreateMagicLinkUser_reusesExistingUserByEmailWithoutOverwritingProfile() {
        User existingUser = this.saveUser("short@example.com", "first.last@example.com");
        existingUser.setName("First");
        existingUser.setLastname("Last");
        userRepository.saveAndFlush(existingUser);

        User result = myUserDetailsService.findOrCreateMagicLinkUser("First.Last@example.com");

        assertEquals(existingUser.getId(), result.getId());
        assertEquals("short@example.com", result.getUsername());
        assertEquals("first.last@example.com", result.getEmail());
        assertEquals("First", result.getName());
        assertEquals("Last", result.getLastname());
        assertEquals(1, userRepository.count());
    }

    @Test
    void findOrCreateMagicLinkUser_doesNotRunReconciliationSideEffects() {
        User userWithEmailAsUsername = this.saveUser("first.last@example.com", "old.email@example.com");
        User userWithCorrectEmail = this.saveUser("student-id@example.com", "first.last@example.com");

        User result = myUserDetailsService.findOrCreateMagicLinkUser("first.last@example.com");

        assertEquals(userWithCorrectEmail.getId(), result.getId());
        assertTrue(userRepository.findById(userWithEmailAsUsername.getId()).get().getEnabled());
        assertTrue(userRepository.findById(userWithCorrectEmail.getId()).get().getEnabled());
    }

    @Test
    void findOrCreateMagicLinkUser_rejectsDisabledUserInsteadOfCreatingGuest() {
        User disabledUser = this.saveUser("disabled@example.com", "disabled@example.com");
        disabledUser.setEnabled(false);
        userRepository.saveAndFlush(disabledUser);

        assertThrows(
                DisabledException.class,
                () -> myUserDetailsService.findOrCreateMagicLinkUser("disabled@example.com")
        );
        assertEquals(1, userRepository.count());
    }

    @Test
    void loadUserByUsername_exposesDisabledState() {
        User disabledUser = this.saveUser("disabled@example.com", "disabled@example.com");
        disabledUser.setEnabled(false);
        userRepository.saveAndFlush(disabledUser);

        UserDetails result = myUserDetailsService.loadUserByUsername("disabled@example.com");

        assertFalse(result.isEnabled());
    }
}
