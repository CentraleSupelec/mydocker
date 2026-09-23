package fr.centralesupelec.thuv.lti;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import fr.centralesupelec.thuv.lti.dtos.OpenIdConfigurationDto;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.OIDCConfig;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two authorization decisions the reported chain hung on, exercised through the real web and
 * security stack: who may register an LTI platform, and what authority a session opened through LTI
 * can exercise once it exists.
 *
 * Transactional so the registration case rolls back the row it writes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        locations = "classpath:application-test.properties",
        // Pin the key pair the context starts with, so this test does not depend on whichever
        // key files happen to be first on the test classpath.
        properties = {
                "lti.private-key-path=classpath:lti-test-private.pem",
                "lti.public-key-path=classpath:lti-test-public.pem"
        }
)
class LtiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "ltiRestTemplate")
    private RestTemplate ltiRestTemplate;

    // Replaced rather than exercised: the real bean fetches the OIDC discovery document over the
    // network at startup, which would make this test depend on an external service being up.
    @MockitoBean(name = OIDCConfig.JWK_BEAN_NAME)
    private JwkProvider jwkProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ComputeTypeRepository computeTypeRepository;

    // The launch checks have their own unit tests; what is under test here is what the handler does
    // with a token that has already passed them.
    @MockitoBean
    private LtiService ltiService;

    @MockitoBean
    private ActivityLogger activityLogger;

    private User createUser(String prefix, String... roleNames) {
        List<Role> roles = new ArrayList<>();
        for (String roleName : roleNames) {
            roles.add(roleRepository.findByName(roleName).orElseThrow());
        }
        String identity = prefix + "-" + UUID.randomUUID() + "@example.org";
        User user = new User();
        user.setEmail(identity);
        user.setUsername(identity);
        user.setName("Test");
        user.setLastname("User");
        user.setEnabled(true);
        user.setRoles(roles);
        return userRepository.saveAndFlush(user);
    }

    private Course createCourseOwnedBy(User creator) {
        ComputeType computeType = computeTypeRepository.findAll().stream().findFirst().orElseThrow();
        Course course = new Course();
        course.setTitle("Course " + UUID.randomUUID());
        course.setDescription("description");
        course.setLink("https://example.org");
        course.setDockerImage("example/image:1");
        course.setCommand("bash");
        course.setComputeType(computeType);
        course.setStatus(CourseStatus.READY);
        course.setCreator(creator);
        return courseRepository.saveAndFlush(course);
    }

    private User createAdministrator() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
        String identity = "admin-" + UUID.randomUUID() + "@example.org";
        User admin = new User();
        admin.setEmail(identity);
        admin.setUsername(identity);
        admin.setName("Real");
        admin.setLastname("Administrator");
        admin.setEnabled(true);
        admin.setRoles(new ArrayList<>(List.of(adminRole)));
        return userRepository.saveAndFlush(admin);
    }

    @Test
    @WithMockUser(roles = "USER")
    void anOrdinaryAccountCannotRegisterAPlatform() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "https://attacker.example.com/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void aTeacherCannotRegisterAPlatformEither() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "https://attacker.example.com/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAnonymousCallerCannotRegisterAPlatform() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "https://attacker.example.com/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdministratorReachesTheHandler() throws Exception {
        // Unique per run so the case does not depend on the class being transactional for its own
        // correctness; the rollback is what keeps the database clean.
        String issuer = "https://platform-" + UUID.randomUUID() + ".example.org";
        OpenIdConfigurationDto configuration = new OpenIdConfigurationDto();
        configuration.setIssuer(issuer);
        configuration.setJwksUri(issuer + "/jwks");
        configuration.setAuthorizationEndpoint(issuer + "/auth");
        configuration.setRegistrationEndpoint(issuer + "/register");
        when(ltiRestTemplate.getForObject(anyString(), eq(OpenIdConfigurationDto.class)))
                .thenReturn(configuration);
        when(ltiService.registerTool(anyString(), any(OpenIdConfigurationDto.class)))
                .thenReturn("client-abc");

        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", issuer + "/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().isOk());

        // Reaching the outbound call is what proves authorization let the request through.
        verify(ltiRestTemplate).getForObject(
                issuer + "/.well-known/openid-configuration", OpenIdConfigurationDto.class
        );
    }

    @Test
    void anLtiSessionCannotReachAnAdministratorEndpoint() throws Exception {
        User admin = createAdministrator();
        String token = jwtTokenProvider.generateToken(admin.getUsername(), admin.getEmail(), TokenOrigin.LTI);

        // The account really is an administrator; the origin of the session is what withholds the
        // authority, which is the property the reported chain turned on.
        mockMvc.perform(get("/admin/contents/").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMagicLinkSessionCannotReachAnAdministratorEndpointEither() throws Exception {
        User admin = createAdministrator();
        String token = jwtTokenProvider.generateToken(
                admin.getUsername(), admin.getEmail(), TokenOrigin.MAGIC_LINK
        );

        mockMvc.perform(get("/admin/contents/").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void theSameAccountReachesItThroughOurOwnIdentityProvider() throws Exception {
        User admin = createAdministrator();
        String token = jwtTokenProvider.generateToken(admin.getUsername(), admin.getEmail(), TokenOrigin.CAS);

        mockMvc.perform(get("/admin/contents/").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void aLaunchClaimingAnAdministratorEmailIsRefusedAtTheEndpoint() throws Exception {
        User admin = createAdministrator();
        String platformToken = JWT.create()
                .withIssuer("https://moodle.example.org")
                .withClaim("email", admin.getEmail())
                .withClaim("given_name", "Attacker")
                .withClaim("family_name", "Attacker")
                .withClaim(LtiConfig.Claims.ROLES, List.of(LtiConfig.Roles.INSTRUCTOR))
                .withClaim(LtiConfig.Claims.MESSAGE_TYPE, "LtiResourceLinkRequest")
                .withClaim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "https://mydocker.example.org")
                .sign(Algorithm.HMAC256("irrelevant-to-this-test"));
        when(ltiService.getValidatedJWT(anyString(), nullable(String.class), any()))
                .thenReturn(JWT.decode(platformToken));

        mockMvc.perform(post(LtiConfig.LAUNCH_PATH)
                        .param("id_token", "platform-token")
                        .param("state", "state-value"))
                .andExpect(status().isForbidden());

        // No session was opened and nothing was recorded as a login.
        verify(activityLogger, never()).log(any(), any());
    }


    @Test
    void aPermissionServiceRouteIsClosedToAnLtiSessionAndOpenToOurOwnProvider() throws Exception {
        // Read access to someone else's course is decided by CoursePermissionService, which reads
        // the principal rather than hasRole(). The account is a genuine administrator; only the
        // origin of the session differs between the two calls.
        User administrator = createAdministrator();
        User someoneElse = createUser("owner", "ROLE_TEACHER");
        Course theirCourse = createCourseOwnedBy(someoneElse);

        String ltiToken = jwtTokenProvider.generateToken(
                administrator.getUsername(), administrator.getEmail(), TokenOrigin.LTI
        );
        mockMvc.perform(get("/admin/courses/" + theirCourse.getId())
                        .header("Authorization", "Bearer " + ltiToken))
                .andExpect(status().isForbidden());

        String casToken = jwtTokenProvider.generateToken(
                administrator.getUsername(), administrator.getEmail(), TokenOrigin.CAS
        );
        mockMvc.perform(get("/admin/courses/" + theirCourse.getId())
                        .header("Authorization", "Bearer " + casToken))
                .andExpect(status().isOk());
    }
}
