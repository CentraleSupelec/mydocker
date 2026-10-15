package fr.centralesupelec.thuv.web;

import com.auth0.jwk.JwkProvider;
import fr.centralesupelec.thuv.ClusterChannels;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.StorageBackend;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionWorkerRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.OIDCConfig;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Status codes through the real routes, security and exception resolution, against the test
 * database. Calls to the Go API go over a real channel to a port nothing listens on, so they fail
 * with UNAVAILABLE as they do when a cluster is down.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        locations = "classpath:application-test.properties",
        // Not the same mocked beans as LtiSecurityIntegrationTest on purpose: a shared context is
        // seeded once at startup, and a Liquibase dropAll in another test class in between leaves
        // the second class without roles.
        properties = {
                "lti.private-key-path=classpath:lti-test-private.pem",
                "lti.public-key-path=classpath:lti-test-public.pem",
                "app.frontend.url=https://front.example.org"
        }
)
class ClusterAndComputeTypeHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "ltiRestTemplate")
    private RestTemplate ltiRestTemplate;

    @MockitoBean(name = OIDCConfig.JWK_BEAN_NAME)
    private JwkProvider jwkProvider;

    @MockitoBean
    private ActivityLogger activityLogger;

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

    @Autowired
    private OVHResourceRepository ovhResourceRepository;

    @Autowired
    private OVHRegionRepository ovhRegionRepository;

    @Autowired
    private OVHRegionWorkerRepository ovhRegionWorkerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterChannels clusterChannels;

    private User createUser(String roleName) {
        String identity = "user-" + UUID.randomUUID() + "@example.org";
        User user = new User();
        user.setEmail(identity);
        user.setUsername(identity);
        user.setName("Test");
        user.setLastname("User");
        user.setEnabled(true);
        user.setRoles(new ArrayList<>(List.of(roleRepository.findByName(roleName).orElseThrow())));
        return userRepository.saveAndFlush(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateToken(user.getUsername(), user.getEmail(), TokenOrigin.CAS);
    }

    private Cluster cluster;
    private ComputeType defaultComputeType;
    private ManagedChannel deadChannel;

    // A cluster of its own whose Go API address refuses connections. Its channel is registered by
    // hand, as the back end does at startup, because channels are only built then. The cluster
    // gets the one compute type with an empty technical name that every cluster must keep.
    @BeforeEach
    void createUnreachableCluster() {
        cluster = new Cluster();
        cluster.setName("unreachable-" + UUID.randomUUID());
        cluster.setGoAddress("127.0.0.1");
        cluster.setGoPort(1);
        cluster = clusterRepository.saveAndFlush(cluster);
        deadChannel = Grpc.newChannelBuilderForAddress("127.0.0.1", 1, InsecureChannelCredentials.create()).build();
        clusterChannels.register(cluster, deadChannel);
        defaultComputeType = new ComputeType();
        defaultComputeType.setDisplayName("Default");
        defaultComputeType.setTechnicalName("");
        defaultComputeType.setStorageBackend(StorageBackend.LOCAL);
        defaultComputeType.setCluster(cluster);
        defaultComputeType = computeTypeRepository.saveAndFlush(defaultComputeType);
    }

    @AfterEach
    void closeChannel() {
        clusterChannels.markStale(cluster.getName());
        deadChannel.shutdownNow();
    }

    private ComputeType defaultComputeType() {
        return defaultComputeType;
    }

    private Course createCourse(User creator, ComputeType computeType) {
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

    private ComputeType createComputeType(Cluster cluster) {
        ComputeType computeType = new ComputeType();
        computeType.setDisplayName("Spare");
        computeType.setTechnicalName("spare-" + UUID.randomUUID());
        computeType.setStorageBackend(StorageBackend.LOCAL);
        computeType.setCluster(cluster);
        return computeTypeRepository.saveAndFlush(computeType);
    }

    private OVHResource createResource() {
        OVHResource resource = new OVHResource();
        resource.setType("b2-7-" + UUID.randomUUID());
        resource.setRamInGo(7L);
        resource.setCoreNumber(2L);
        return ovhResourceRepository.saveAndFlush(resource);
    }

    private OVHRegion createRegion() {
        OVHRegion region = new OVHRegion();
        region.setRegion("R" + UUID.randomUUID().toString().substring(0, 8));
        region.setImageId("image");
        return ovhRegionRepository.saveAndFlush(region);
    }

    @Test
    @WithMockUser(roles = "BASIC_AUTH")
    void launchOnAnUnreachableClusterIs503() throws Exception {
        User student = createUser("ROLE_USER");
        Course course = createCourse(createUser("ROLE_TEACHER"), defaultComputeType());

        mockMvc.perform(post("/container/" + course.getId() + "/" + student.getId() + "/init"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void logsOnAnUnreachableClusterAre503() throws Exception {
        User student = createUser("ROLE_USER");
        Course course = createCourse(createUser("ROLE_TEACHER"), defaultComputeType());

        mockMvc.perform(get("/docker/logs/" + course.getId() + "/tasks").header("Authorization", bearer(student)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void createAComputeTypeWithRegionsStoresTheRegions() throws Exception {
        User admin = createUser("ROLE_ADMIN");
        Long clusterId = defaultComputeType().getCluster().getId();
        OVHResource resource = createResource();
        OVHRegion region = createRegion();
        String technicalName = "gpu-" + UUID.randomUUID();
        String body = """
                {"displayName": "GPU", "technicalName": "%s", "storageBackend": "LOCAL", "cluster": %d,
                 "minIdleNodesCount": 0, "maxNodesCount": 1,
                 "autoscalingResourcesRegions": [{"ovhResourceId": %d, "regions": ["%s"]}]}
                """.formatted(technicalName, clusterId, resource.getId(), region.getRegion());

        mockMvc.perform(post("/admin/compute-types")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM resource r JOIN compute_types c ON c.id = r.compute_type_id "
                        + "WHERE c.technical_name = ? AND r.resource_id = ? AND r.region_region = ?",
                Integer.class, technicalName, resource.getId(), region.getRegion()
        );
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void deleteAComputeTypeUsedByACourseIs409() throws Exception {
        User admin = createUser("ROLE_ADMIN");
        ComputeType computeType = createComputeType(defaultComputeType().getCluster());
        createCourse(admin, computeType);

        mockMvc.perform(delete("/admin/compute-types/" + computeType.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteAComputeTypeUsedByAWorkerIs409() throws Exception {
        User admin = createUser("ROLE_ADMIN");
        ComputeType computeType = createComputeType(defaultComputeType().getCluster());
        OVHRegionWorker worker = new OVHRegionWorker()
                .setRegion(createRegion())
                .setRessource(createResource())
                .setCount(1L)
                .setComputeType(computeType);
        ovhRegionWorkerRepository.saveAndFlush(worker);

        mockMvc.perform(delete("/admin/compute-types/" + computeType.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteAnUnusedComputeTypeRemovesIt() throws Exception {
        User admin = createUser("ROLE_ADMIN");
        ComputeType computeType = createComputeType(defaultComputeType().getCluster());

        mockMvc.perform(delete("/admin/compute-types/" + computeType.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM compute_types WHERE id = ?", Integer.class, computeType.getId()
        );
        assertThat(rows).isZero();
    }
}
