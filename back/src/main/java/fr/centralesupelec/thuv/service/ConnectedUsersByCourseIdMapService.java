package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.protobuf.Empty;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConnectedUsersByCourseIdMapService {
    private static final Logger logger = LoggerFactory.getLogger(CourseInfraRequestService.class);
    private final ManagedChannel channel;

    public Map<String, Integer> getConnectedUsers() {
        Map<String, Integer>  connectedUsersByCourseIdMap = null;
        containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(channel);
        try {
            connectedUsersByCourseIdMap = stub.getConnectedUsersByCourseIdMap(Empty.getDefaultInstance())
                .getConnectedUsersByCourseIdMapMap();
            logger.info("Retrived running services count by courseId");
        } catch (StatusRuntimeException e) {
            logger.warn(
                    "Could not retrieve running services by courseId"
            );
        }
        return connectedUsersByCourseIdMap;
    }
}
