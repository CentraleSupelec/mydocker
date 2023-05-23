package fr.centralesupelec.thuv.test_connection_scheduler;

import fr.centralesupelec.gRPC.NodeIPRequest;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NodeIPRequestService {
    private static final Logger logger = LoggerFactory.getLogger(NodeIPRequestService.class);
    private final Channel channel;

    public String getNodeIP(String userId, String courseId) {
        String nodeIP = null;
        containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(channel);
        try {
             nodeIP = stub.getNodeIP(
                    NodeIPRequest.newBuilder()
                            .setCourseID(courseId)
                            .setUserID(userId)
                            .build()
            ).getIPAddress();
             logger.debug(String.format(
                     "Retrieved IP Address for user '%s' / course '%s' : '%s'",
                     userId, courseId, nodeIP
             ));
        } catch (StatusRuntimeException e) {
            logger.warn(String.format(
                    "Could not fetch IP Address for user '%s' / course '%s' (%d) : %s",
                    userId, courseId, e.getStatus().getCode().value(), e.getStatus().getDescription()
                    ));
        }
        return nodeIP;
    }
}
