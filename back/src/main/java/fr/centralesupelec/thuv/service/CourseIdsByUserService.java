package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.CourseIds;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.protobuf.Empty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseIdsByUserService {
    private static final Logger logger = LoggerFactory.getLogger(CourseIdsByUserService.class);
    private final Map<String, ManagedChannel> channels;
    private final ClusterRepository clusterRepository;

    public Map<String, List<String>> getCourseIdsByUser() {
        List<Cluster> clusters = clusterRepository.findAll();
        Map<String, List<String>> result = new HashMap<>();

        for (Cluster cluster : clusters) {
            try {
                containerServiceGrpc.containerServiceBlockingStub stub =
                    containerServiceGrpc.newBlockingStub(channels.get(cluster.getName()));

                Map<String, CourseIds> courseIdsByUserIdMap =
                    stub.getCourseIdsByUser(Empty.getDefaultInstance())
                        .getCourseIdsByUserIdMapMap();

                for (Map.Entry<String, CourseIds> entry : courseIdsByUserIdMap.entrySet()) {
                    result
                        .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .addAll(entry.getValue().getCourseIdsList());
                }
                logger.info("Retrieved active courses by userId for cluster '{}''", cluster.getName());
            } catch (StatusRuntimeException e) {
                logger.warn(
                        "Could not get active courses by userId for cluster '{}'' / '{}' ({}) : {}",
                        cluster.getName(), e.getStatus().getCode().value(), e.getStatus().getDescription()
                );
            }
        }

        return result;
    }
}
