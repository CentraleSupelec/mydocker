package fr.centralesupelec.thuv.web.api;

import fr.centralesupelec.gRPC.InitAutoscalingRequest;
import fr.centralesupelec.gRPC.InstanceRegionsConfig;
import fr.centralesupelec.gRPC.OwnerAutoscalingConfig;
import fr.centralesupelec.gRPC.ScalingRegion;
import fr.centralesupelec.thuv.autoscaling.service.InitAutoscalingService;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serves the Go API of a cluster the complete list of the autoscaled compute types of that
 * cluster, the same owners the back pushes with initAutoscaling.
 */
@RestController("apiAutoscalingTypesController")
@RequestMapping("/internal/autoscaling")
public class AutoscalingTypesController {
    private final ClusterRepository clusterRepository;
    private final InitAutoscalingService initAutoscalingService;
    private final String expectedToken;

    public AutoscalingTypesController(
        ClusterRepository clusterRepository,
        InitAutoscalingService initAutoscalingService,
        @Value("${autoscaling_pull_token:#{null}}") String expectedToken
    ) {
        this.clusterRepository = clusterRepository;
        this.initAutoscalingService = initAutoscalingService;
        this.expectedToken = expectedToken;
    }

    @GetMapping("/{clusterName}/compute-types")
    public ResponseEntity<Object> getAutoscalingTypes(
        @PathVariable("clusterName") String clusterName,
        @RequestHeader(value = "X-Autoscaling-Token", required = false) String token
    ) {
        if (!isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Cluster> cluster = clusterRepository.findByName(clusterName);
        if (cluster.isEmpty()) {
            // Never an empty list: the Go API would destroy every VM it has.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        InitAutoscalingRequest request = initAutoscalingService.buildRequest(cluster.get());
        Map<String, Object> owners = new LinkedHashMap<>();
        request.getOwnersMap().forEach((name, owner) -> owners.put(name, toJson(owner)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("owners", owners);
        return ResponseEntity.ok(body);
    }

    private boolean isTokenValid(String token) {
        if (expectedToken == null || expectedToken.isEmpty() || token == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static Map<String, Object> toJson(OwnerAutoscalingConfig owner) {
        List<Map<String, Object>> instancesRegions = new ArrayList<>();
        for (InstanceRegionsConfig instance : owner.getInstancesRegionsList()) {
            List<Map<String, Object>> regions = new ArrayList<>();
            for (ScalingRegion region : instance.getRegionsList()) {
                Map<String, Object> regionJson = new LinkedHashMap<>();
                regionJson.put("imageId", region.getImageId());
                regionJson.put("region", region.getRegion());
                regions.add(regionJson);
            }
            Map<String, Object> instanceJson = new LinkedHashMap<>();
            instanceJson.put("instanceType", instance.getInstanceType());
            instanceJson.put("regions", regions);
            instancesRegions.add(instanceJson);
        }
        Map<String, Object> ownerJson = new LinkedHashMap<>();
        ownerJson.put("minIdleNodesCount", owner.getMinIdleNodesCount());
        ownerJson.put("maxNodesCount", owner.getMaxNodesCount());
        ownerJson.put("manualNodesCount", owner.getManualNodesCount());
        ownerJson.put("instancesRegions", instancesRegions);
        return ownerJson;
    }
}
