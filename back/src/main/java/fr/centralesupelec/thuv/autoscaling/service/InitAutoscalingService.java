package fr.centralesupelec.thuv.autoscaling.service;

import fr.centralesupelec.gRPC.InitAutoscalingRequest;
import fr.centralesupelec.gRPC.InitAutoscalingResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.mappers.ComputeTypeMapper;
import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.repository.ComputeTypeRepository;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InitAutoscalingService {
    private static final Logger logger = LoggerFactory.getLogger(InitAutoscalingService.class);
    private final ManagedChannel channel;
    private final ComputeTypeRepository computeTypeRepository;
    private final ComputeTypeMapper computeTypeMapper;
    private boolean isAutoscalingInitialized = false;

    public void sendInitRequest() throws Exception {
        containerServiceGrpc.containerServiceBlockingStub asyncStub = containerServiceGrpc.newBlockingStub(channel);
        List<ComputeType> computeTypes = computeTypeRepository.findAll();
        InitAutoscalingRequest request = InitAutoscalingRequest
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
        logger.debug("Initializing autoscaling config {}", request);
        InitAutoscalingResponse initAutoscalingResponse = asyncStub.initAutoscaling(request);
        if (!initAutoscalingResponse.getError().isEmpty()) {
            throw new Exception(initAutoscalingResponse.getError());
        }
    }

    public void initAutoscaling() {
        if (!isAutoscalingInitialized) {
            try {
                sendInitRequest();
                isAutoscalingInitialized = true;
                logger.info("Successfully initialized autoscaling config");
            } catch (Exception e) {
                logger.error(String.format("Could not initialize autoscaling config : %s", e.getMessage()), e);
            }
        } else {
            logger.debug("Autoscaling already initialized, skipping");
        }
    }
}
