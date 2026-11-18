package fr.centralesupelec.thuv.web.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.gRPC.InitAutoscalingRequest;
import fr.centralesupelec.gRPC.InitAutoscalingResponse;
import fr.centralesupelec.gRPC.InstanceRegionsConfig;
import fr.centralesupelec.gRPC.OwnerAutoscalingConfig;
import fr.centralesupelec.gRPC.ScalingRegion;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.mappers.ClusterMapper;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.ResourceRegion;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHRegionRepository;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import fr.centralesupelec.thuv.scale_up.repository.ResourceRegionRepository;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The Go API pulls the complete list of the autoscaled types of its cluster at startup. The
 * answer is the owners the back pushes with initAutoscaling, as JSON: numbers as JSON numbers,
 * field names those of the InitAutoscalingRequest messages.
 */
class AutoscalingTypesControllerTest {
    private static final String TOKEN = "secret";

    private final ObjectMapper json = new ObjectMapper();
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);
    private final ComputeTypeRepository computeTypeRepository = mock(ComputeTypeRepository.class);
    private final ComputeTypeMapper computeTypeMapper = new ComputeTypeMapper(
            mock(OVHRegionRepository.class),
            mock(OVHResourceRepository.class),
            mock(ResourceRegionRepository.class),
            mock(ClusterRepository.class),
            new ClusterMapper());
    private final InitAutoscalingService initAutoscalingService =
            new InitAutoscalingService(Map.of(), computeTypeRepository, computeTypeMapper, clusterRepository);
    private final AutoscalingTypesController controller =
            new AutoscalingTypesController(clusterRepository, initAutoscalingService, TOKEN);

    private ManagedChannel channel;
    private Server server;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    private static Cluster cluster(String name) {
        return new Cluster().setId(1L).setName(name).setGoAddress("go").setGoPort(50000).setDeploymentEnabled(true);
    }

    private static OVHRegion region(String region, String imageId) {
        return new OVHRegion().setRegion(region).setImageId(imageId);
    }

    private static ComputeType type(String technicalName, long minIdle, long max, long manual, String instanceType, OVHRegion... regions) {
        ComputeType computeType = new ComputeType()
                .setTechnicalName(technicalName)
                .setMinIdleNodesCount(minIdle)
                .setMaxNodesCount(max)
                .setManualNodesCount(manual);
        OVHResource resource = new OVHResource().setType(instanceType);
        for (OVHRegion region : regions) {
            computeType.getAutoscalingResourcesRegions().add(
                    new ResourceRegion().setComputeType(computeType).setResource(resource).setRegion(region));
        }
        return computeType;
    }

    private JsonNode body(ResponseEntity<Object> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.valueToTree(response.getBody());
    }

    private static List<String> names(JsonNode object) {
        List<String> names = new ArrayList<>();
        Iterator<String> it = object.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    private static Set<String> imageRegionPairs(JsonNode regions) {
        Set<String> pairs = new HashSet<>();
        for (JsonNode region : regions) {
            pairs.add(region.get("imageId").asText() + "/" + region.get("region").asText());
        }
        return pairs;
    }

    @Test
    void theAnswerHoldsTheCompleteListOfTheAutoscaledTypesOfTheCluster() {
        Cluster cluster = cluster("a");
        when(clusterRepository.findByName("a")).thenReturn(Optional.of(cluster));
        when(computeTypeRepository.findByCluster(cluster)).thenReturn(List.of(
                type("gpu-a", 1, 3, 2, "b3-8", region("GRA11", "img-1"), region("SBG5", "img-2")),
                type("cpu", 0, 5, 0, "b2-7", region("GRA11", "img-1")),
                // max 0: not autoscaled, so not in the list (its VMs go to zero)
                type("off", 0, 0, 0, "b2-7", region("GRA11", "img-1")),
                // no region: not autoscaled either
                type("no-region", 0, 4, 0, "b2-7")));

        JsonNode body = body(controller.getAutoscalingTypes("a", TOKEN));

        assertThat(names(body)).containsExactly("owners");
        JsonNode owners = body.get("owners");
        assertThat(names(owners)).containsExactlyInAnyOrder("gpu-a", "cpu");
        JsonNode gpu = owners.get("gpu-a");
        assertThat(gpu.get("minIdleNodesCount").isIntegralNumber()).isTrue();
        assertThat(gpu.get("minIdleNodesCount").asLong()).isEqualTo(1L);
        assertThat(gpu.get("maxNodesCount").asLong()).isEqualTo(3L);
        assertThat(gpu.get("manualNodesCount").asLong()).isEqualTo(2L);
        assertThat(gpu.get("instancesRegions")).hasSize(1);
        assertThat(gpu.get("instancesRegions").get(0).get("instanceType").asText()).isEqualTo("b3-8");
        assertThat(imageRegionPairs(gpu.get("instancesRegions").get(0).get("regions")))
                .containsExactlyInAnyOrder("img-1/GRA11", "img-2/SBG5");
        assertThat(owners.get("cpu").get("maxNodesCount").asLong()).isEqualTo(5L);
    }

    // A cluster that has no autoscaled type is a complete list of zero types. A cluster the back
    // does not know is not an empty list: the Go API would then destroy every VM it has.
    @Test
    void aClusterWithNoAutoscaledTypeAnswersAnExplicitEmptyListAndAnUnknownClusterIsNotFound() {
        Cluster quiet = cluster("quiet");
        when(clusterRepository.findByName("quiet")).thenReturn(Optional.of(quiet));
        when(computeTypeRepository.findByCluster(quiet)).thenReturn(List.of(type("off", 0, 0, 0, "b2-7", region("GRA11", "img-1"))));
        when(clusterRepository.findByName("nobody")).thenReturn(Optional.empty());

        JsonNode empty = body(controller.getAutoscalingTypes("quiet", TOKEN));
        ResponseEntity<Object> unknown = controller.getAutoscalingTypes("nobody", TOKEN);

        assertThat(empty.has("owners")).isTrue();
        assertThat(empty.get("owners").isObject()).isTrue();
        assertThat(empty.get("owners")).isEmpty();
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknown.getBody()).isNull();
    }

    @Test
    void aMissingWrongOrUnconfiguredTokenIsRefusedBeforeAnythingIsRead() {
        for (String sent : new String[] {null, "", "wrong", "secre", "secret ", "SECRET"}) {
            ResponseEntity<Object> response = controller.getAutoscalingTypes("a", sent);

            assertThat(response.getStatusCode()).as("token %s", sent).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNull();
        }
        // No token configured: the endpoint is closed, even to an empty or a null token.
        for (String configured : new String[] {null, ""}) {
            AutoscalingTypesController closed =
                    new AutoscalingTypesController(clusterRepository, initAutoscalingService, configured);
            for (String sent : new String[] {null, "", "secret"}) {
                assertThat(closed.getAutoscalingTypes("a", sent).getStatusCode())
                        .as("configured %s, sent %s", configured, sent)
                        .isEqualTo(HttpStatus.FORBIDDEN);
            }
        }
        verifyNoInteractions(clusterRepository, computeTypeRepository);
    }

    // The pull and the push must never disagree: same cluster, same types, same owners.
    @Test
    void thePullAndThePushCarryTheSameOwnersForTheSameCluster() throws Exception {
        AtomicReference<InitAutoscalingRequest> pushed = new AtomicReference<>();
        String name = "autoscaling-pull-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new containerServiceGrpc.containerServiceImplBase() {
                    @Override
                    public void initAutoscaling(InitAutoscalingRequest request, StreamObserver<InitAutoscalingResponse> out) {
                        pushed.set(request);
                        out.onNext(InitAutoscalingResponse.getDefaultInstance());
                        out.onCompleted();
                    }
                }).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        InitAutoscalingService service =
                new InitAutoscalingService(Map.of("a", channel), computeTypeRepository, computeTypeMapper, clusterRepository);
        AutoscalingTypesController pullController = new AutoscalingTypesController(clusterRepository, service, TOKEN);
        Cluster cluster = cluster("a");
        when(clusterRepository.findByName("a")).thenReturn(Optional.of(cluster));
        when(computeTypeRepository.findByCluster(cluster)).thenReturn(List.of(
                type("gpu-a", 1, 3, 2, "b3-8", region("GRA11", "img-1"), region("SBG5", "img-2")),
                type("cpu", 0, 5, 0, "b2-7", region("GRA11", "img-1")),
                type("off", 0, 0, 0, "b2-7", region("GRA11", "img-1"))));

        service.sendInitRequest(cluster);
        JsonNode pulled = body(pullController.getAutoscalingTypes("a", TOKEN)).get("owners");

        InitAutoscalingRequest push = pushed.get();
        assertThat(push).isNotNull();
        assertThat(names(pulled)).containsExactlyInAnyOrderElementsOf(push.getOwnersMap().keySet());
        for (Map.Entry<String, OwnerAutoscalingConfig> owner : push.getOwnersMap().entrySet()) {
            JsonNode node = pulled.get(owner.getKey());
            assertThat(node.get("minIdleNodesCount").asLong()).isEqualTo(owner.getValue().getMinIdleNodesCount());
            assertThat(node.get("maxNodesCount").asLong()).isEqualTo(owner.getValue().getMaxNodesCount());
            assertThat(node.get("manualNodesCount").asLong()).isEqualTo(owner.getValue().getManualNodesCount());
            assertThat(node.get("instancesRegions")).hasSize(owner.getValue().getInstancesRegionsCount());
            for (InstanceRegionsConfig instance : owner.getValue().getInstancesRegionsList()) {
                Set<String> expected = new HashSet<>();
                for (ScalingRegion region : instance.getRegionsList()) {
                    expected.add(region.getImageId() + "/" + region.getRegion());
                }
                JsonNode pulledInstance = null;
                for (JsonNode candidate : node.get("instancesRegions")) {
                    if (candidate.get("instanceType").asText().equals(instance.getInstanceType())) {
                        pulledInstance = candidate;
                    }
                }
                assertThat(pulledInstance).as("instance type %s", instance.getInstanceType()).isNotNull();
                assertThat(imageRegionPairs(pulledInstance.get("regions"))).isEqualTo(expected);
            }
        }
    }

    // The values the Go API relies on: path, header name, path variable name.
    @Test
    void theRouteAndTheTokenHeaderAreTheOnesTheGoApiUses() throws Exception {
        Class<?> type = AutoscalingTypesController.class;
        assertThat(AnnotatedElementUtils.hasAnnotation(type, RestController.class)).isTrue();
        RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        assertThat(base).isNotNull();
        assertThat(base.path()).containsExactly("/internal/autoscaling");

        Method method = type.getMethod("getAutoscalingTypes", String.class, String.class);
        GetMapping get = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        assertThat(get).isNotNull();
        assertThat(get.path()).containsExactly("/{clusterName}/compute-types");
        PathVariable clusterName = AnnotatedElementUtils.findMergedAnnotation(method.getParameters()[0], PathVariable.class);
        assertThat(clusterName).isNotNull();
        assertThat(clusterName.name()).isEqualTo("clusterName");
        RequestHeader header = AnnotatedElementUtils.findMergedAnnotation(method.getParameters()[1], RequestHeader.class);
        assertThat(header).isNotNull();
        assertThat(header.name()).isEqualTo("X-Autoscaling-Token");
        assertThat(header.required()).isFalse();
    }
}
