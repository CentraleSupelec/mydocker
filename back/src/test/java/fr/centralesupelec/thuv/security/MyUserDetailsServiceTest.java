package fr.centralesupelec.thuv.security;

import static org.junit.jupiter.api.Assertions.*;

import fr.centralesupelec.thuv.model.User;
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
    private MyUserDetailsService myUserDetailsService;

    private static final String usernameCs = "11442@cs.fr";
    private static final String emailCs = "prenom.nom@cs.fr";
    private static final String oldEmailUps = "prenom.nom@ups.fr";
    private static final String newStudentEmailUps = "prenom.nom@student-ups.fr";

    protected User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(email);
        user.setLastname(email);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void findUser_noUserInDBNoUsernameFirst() throws Exception {

        User result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(null, result.getId());
        result.setLastname(emailCs).setName(emailCs);
        User user = userRepository.saveAndFlush(result);
        Long id = user.getId();

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(id, result.getId());
        assertEquals(emailCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());

        result = myUserDetailsService.findUser(usernameCs, emailCs);
        userRepository.saveAndFlush(result);

        assertEquals(id, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(id, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
    }

    @Test
    void findUser_noUserInDBUsernameFirst() throws Exception {

        User result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(null, result.getId());

        User user = this.saveUser(usernameCs, emailCs);
        Long id = user.getId();

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(id, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());

        result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(id, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
    }

    @Test
    void findUser_UsernameInDBNoUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, usernameCs);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(usernameCs).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(null, result.getId());

        User user = this.saveUser(emailCs, emailCs);
        Long createdUserId = user.getId();

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(createdUserId, result.getId());
        assertEquals(emailCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());

        result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());

        User previousUser = userRepository.findById(createdUserId).get();
        assertFalse(previousUser.getEnabled());
    }

   @Test
    void findUser_UsernameInDBUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, usernameCs);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(usernameCs).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
        result.setLastname(emailCs).setName(emailCs);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
    }

    @Test
    void findUser_EmailInDBNoUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, emailCs);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(emailCs).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(emailCs, result.getEmail());
        result.setLastname(emailCs).setName(emailCs);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(existingId, result.getId());

        result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
    }

    @Test
    void findUser_EmailInDBUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, emailCs);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(emailCs).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
        result.setLastname(emailCs).setName(emailCs);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(existingId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
    }

    @Test
    void findUser_BothInDBNoUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, emailCs);
        statement.executeUpdate();
        User existingEmailUser = userRepository.findByUsername(emailCs).get();
        Long existingEmailId = existingEmailUser.getId();
        statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, usernameCs);
        statement.executeUpdate();
        User existingUsernameUser = userRepository.findByUsername(usernameCs).get();
        Long existingUsernameId = existingUsernameUser.getId();

        User result = myUserDetailsService.findUser(null, emailCs);

        assertEquals(existingEmailId, result.getId());
        assertEquals(emailCs, result.getEmail());
        result.setLastname(emailCs).setName(emailCs);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(usernameCs, emailCs);

        assertEquals(existingUsernameId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
        User previousUser = userRepository.findById(existingEmailId).get();
        assertFalse(previousUser.getEnabled());
    }

    @Test
    void findUser_BothInDBUsernameFirst() throws Exception {
        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, emailCs);
        statement.executeUpdate();
        User existingEmailUser = userRepository.findByUsername(emailCs).get();
        Long existingEmailId = existingEmailUser.getId();

        statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username) VALUES (?)");
        statement.setString(1, usernameCs);
        statement.executeUpdate();
        User existingUsernameUser = userRepository.findByUsername(usernameCs).get();
        Long existingUsernameId = existingUsernameUser.getId();
        statement = this.dataSource.getConnection().prepareStatement("SELECT * FROM users");
        logger.info("Initial Users in DB:");
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            logger.info("id: " + rs.getLong("id"));
            logger.info("username: " + rs.getString("username"));
            logger.info("email: " + rs.getString("email"));
            logger.info("enabled: " + rs.getBoolean("enabled"));
            logger.info("name: " + rs.getString("name"));
            logger.info("lastname: " + rs.getString("lastname"));
        }

        User result = myUserDetailsService.findUser(usernameCs, emailCs);
        assertEquals(existingUsernameId, result.getId());
        assertEquals(emailCs, result.getEmail());
        assertEquals(usernameCs, result.getUsername());
        result.setLastname(emailCs).setName(emailCs);
        userRepository.save(result);
        userRepository.flush();

        logger.info("Users in DB:");
        statement = this.dataSource.getConnection().prepareStatement("SELECT * FROM users");
        rs = statement.executeQuery();
        while (rs.next()) {
            logger.info("id: " + rs.getLong("id"));
            logger.info("username: " + rs.getString("username"));
            logger.info("email: " + rs.getString("email"));
            logger.info("enabled: " + rs.getBoolean("enabled"));
            logger.info("name: " + rs.getString("name"));
            logger.info("lastname: " + rs.getString("lastname"));
        }


        result = myUserDetailsService.findUser(null, emailCs);
        assertEquals(existingUsernameId, result.getId());
        assertEquals(usernameCs, result.getUsername());
        assertEquals(emailCs, result.getEmail());
        User previousUser = userRepository.findById(existingEmailId).get();
        assertFalse(previousUser.getEnabled());
        logger.info("Final Users in DB:");
        statement = this.dataSource.getConnection().prepareStatement("SELECT * FROM users");
        rs = statement.executeQuery();
        while (rs.next()) {
            logger.info("id: " + rs.getLong("id"));
            logger.info("username: " + rs.getString("username"));
            logger.info("email: " + rs.getString("email"));
            logger.info("enabled: " + rs.getBoolean("enabled"));
            logger.info("name: " + rs.getString("name"));
            logger.info("lastname: " + rs.getString("lastname"));
        }
    }

    @Test
    void findUser_UpsInDBConnectWithUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)");
        statement.setString(1, oldEmailUps);
        statement.setString(2, newStudentEmailUps);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(oldEmailUps).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(null, oldEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(oldEmailUps, result.getEmail());
        assertEquals(oldEmailUps, result.getUsername());
        result.setLastname(newStudentEmailUps).setName(newStudentEmailUps);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(oldEmailUps, oldEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(oldEmailUps, result.getEmail());
        assertEquals(oldEmailUps, result.getUsername());
    }

    @Test
    void findUser_UpsInDBConnectWithEmailFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)");
        statement.setString(1, oldEmailUps);
        statement.setString(2, newStudentEmailUps);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(oldEmailUps).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(oldEmailUps, oldEmailUps);
        logger.info("result: " + result);
        assertEquals(existingId, result.getId());
        assertEquals(oldEmailUps, result.getEmail());
        assertEquals(oldEmailUps, result.getUsername());
        result.setLastname(newStudentEmailUps).setName(newStudentEmailUps);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(null, oldEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(oldEmailUps, result.getEmail());
        assertEquals(oldEmailUps, result.getUsername());
    }

    @Test
    void findUser_UpsInDBConnectEmailUpsWithUsernameFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)");
        statement.setString(1, oldEmailUps);
        statement.setString(2, newStudentEmailUps);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(oldEmailUps).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(null, newStudentEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(newStudentEmailUps, result.getEmail());
        assertEquals(oldEmailUps, result.getUsername());
        result.setLastname(newStudentEmailUps).setName(newStudentEmailUps);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(newStudentEmailUps, newStudentEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(newStudentEmailUps, result.getEmail());
        assertEquals(newStudentEmailUps, result.getUsername());
    }

    @Test
    void findUser_UpsInDBConnectEmailUpsWithEmailFirst() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)");
        statement.setString(1, oldEmailUps);
        statement.setString(2, newStudentEmailUps);
        statement.executeUpdate();
        User existingUser = userRepository.findByUsername(oldEmailUps).get();
        Long existingId = existingUser.getId();

        User result = myUserDetailsService.findUser(newStudentEmailUps, newStudentEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(newStudentEmailUps, result.getEmail());
        assertEquals(newStudentEmailUps, result.getUsername());
        result.setLastname(newStudentEmailUps).setName(newStudentEmailUps);
        userRepository.saveAndFlush(result);

        result = myUserDetailsService.findUser(null, newStudentEmailUps);

        assertEquals(existingId, result.getId());
        assertEquals(newStudentEmailUps, result.getEmail());
        assertEquals(newStudentEmailUps, result.getUsername());
    }

    @Test
    void findUser_UpsBothInDBConnectWithEmail() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)");
        statement.setString(1, oldEmailUps);
        statement.setString(2, newStudentEmailUps);
        statement.executeUpdate();
        statement.setString(1, newStudentEmailUps);
        statement.setString(2, null);
        statement.executeUpdate();
        User existingUserWithUsername = userRepository.findByUsername(oldEmailUps).get();
        Long existingUserWithUsernameId = existingUserWithUsername.getId();
        User existingUserWithEmail = userRepository.findByUsername(newStudentEmailUps).get();
        Long existingUserWithEmailId = existingUserWithEmail.getId();

        User result = myUserDetailsService.findUser(null, newStudentEmailUps);

        assertEquals(existingUserWithEmailId, result.getId());
        assertEquals(newStudentEmailUps, result.getEmail());
        assertEquals(newStudentEmailUps, result.getUsername());
        result.setLastname(newStudentEmailUps).setName(newStudentEmailUps);
        userRepository.saveAndFlush(result);
        User previousUser = userRepository.findById(existingUserWithUsernameId).get();
        assertFalse(previousUser.getEnabled());
    }

    @Test
    void findUser_UpsBothInDBConnectWithUsername() throws Exception {

        PreparedStatement statement = this.dataSource.getConnection()
                .prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)");
        statement.setString(1, oldEmailUps);
        statement.setString(2, newStudentEmailUps);
        statement.executeUpdate();
        statement.setString(1, newStudentEmailUps);
        statement.setString(2, null);
        statement.executeUpdate();
        User existingUserWithUsername = userRepository.findByUsername(oldEmailUps).get();
        Long existingUserWithUsernameId = existingUserWithUsername.getId();
        User existingUserWithEmail = userRepository.findByUsername(newStudentEmailUps).get();
        Long existingUserWithEmailId = existingUserWithEmail.getId();

        User result = myUserDetailsService.findUser(newStudentEmailUps, newStudentEmailUps);

        assertEquals(existingUserWithEmailId, result.getId());
        assertEquals(newStudentEmailUps, result.getEmail());
        assertEquals(newStudentEmailUps, result.getUsername());
        result.setLastname(newStudentEmailUps).setName(newStudentEmailUps);
        userRepository.saveAndFlush(result);
        User previousUser = userRepository.findById(existingUserWithUsernameId).get();
        assertFalse(previousUser.getEnabled());
    }
}
