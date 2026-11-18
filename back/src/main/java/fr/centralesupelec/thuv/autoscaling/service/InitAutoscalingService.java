package fr.centralesupelec.thuv.autoscaling.service;

import fr.centralesupelec.gRPC.InitAutoscalingRequest;
import fr.centralesupelec.gRPC.InitAutoscalingResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import io.grpc.ManagedChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InitAutoscalingService {
    private static final Logger logger = LoggerFactory.getLogger(InitAutoscalingService.class);
    private final Map<String, ManagedChannel> channels;
    private final ComputeTypeRepository computeTypeRepository;
    private final ComputeTypeMapper computeTypeMapper;
    private Map<String, Boolean> isAutoscalingInitializedMap = new HashMap<>();

    private final ClusterRepository clusterRepository;

    @PostConstruct
    public void init() {
        List<Cluster> clusters = clusterRepository.findAll();

        for (Cluster cluster : clusters) {
            isAutoscalingInitializedMap.put(cluster.getName(), false);
        }
    }

    public void setIsAutoscalingInitialized(String cluster, Boolean isAutoscalingInitialized) {
        isAutoscalingInitializedMap.put(cluster, isAutoscalingInitialized);
    }

    /**
     * The complete autoscaling config of a cluster: every autoscaled compute type, keyed by
     * technical name. Both the push (initAutoscaling) and the pull endpoint use it.
     */
    public InitAutoscalingRequest buildRequest(Cluster cluster) {
        List<ComputeType> computeTypes = computeTypeRepository.findByCluster(cluster);
        return InitAutoscalingRequest
                .newBuilder()
                .putAllOwners(
                        computeTypes
                                .stream()
                                .filter(ComputeType::isAutoscalingConfigured)
                                .collect(
                                        Collectors.toMap(
                                                ComputeType::getTechnicalName,
                                                computeTypeMapper::convertComputeTypeToOwner
                                        )
                                )
                )
                .build();
    }

    public void sendInitRequest(Cluster cluster) throws Exception {
        containerServiceGrpc.containerServiceBlockingStub asyncStub = containerServiceGrpc.newBlockingStub(channels.get(cluster.getName()));
        InitAutoscalingRequest request = buildRequest(cluster);
        logger.debug("Initializing autoscaling config {}", request);
        InitAutoscalingResponse initAutoscalingResponse = asyncStub.initAutoscaling(request);
        if (!initAutoscalingResponse.getError().isEmpty()) {
            throw new Exception(initAutoscalingResponse.getError());
        }
    }

    public void initAutoscalingByCluster(Cluster cluster) {
        if (!isAutoscalingInitializedMap.get(cluster.getName()) && cluster.getDeploymentEnabled()) {
            try {
                sendInitRequest(cluster);
                isAutoscalingInitializedMap.put(cluster.getName(), true);
                logger.info(String.format("Successfully initialized autoscaling config for cluster %s", cluster.getName()));
            } catch (Exception e) {
                logger.error(String.format("Could not initialize autoscaling config for cluster %s : %s", e.getMessage()), cluster.getName(), e);
            }
        } else {
            logger.debug(String.format("Autoscaling already initialized for cluster %s, skipping", cluster.getName()));
        }
    }
}
