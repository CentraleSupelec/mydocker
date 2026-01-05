package fr.centralesupelec.thuv.service;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConnectedUsersByCourseIdMapService {
    private static final Logger logger = LoggerFactory.getLogger(CourseInfraRequestService.class);
    private final Map<String, ManagedChannel> channels;
    private final ClusterRepository clusterRepository;

    public Map<String, Integer> getConnectedUsers() {
        Map<String, Integer>  connectedUsersByCourseIdMap = new HashMap<>();
        List<Cluster> clusters = clusterRepository.findAll();

        for (Cluster cluster : clusters) {
            containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(channels.get(cluster.getName()));
            try {
                Map<String, Integer> connectedUsersByCourseIdMapForCurrentCluster = stub.getConnectedUsersByCourseIdMap(Empty.getDefaultInstance())
                    .getConnectedUsersByCourseIdMapMap();
                connectedUsersByCourseIdMapForCurrentCluster.forEach((courseId, count) ->
                    connectedUsersByCourseIdMap.merge(courseId, count, Integer::sum)
            );
                logger.info("Retrived running services count by courseId");
            } catch (StatusRuntimeException e) {
                logger.warn(
                        "Could not retrieve running services by courseId"
                );
            }
        }
        
        return connectedUsersByCourseIdMap;
    }
}
