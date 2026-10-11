package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.LogRequest;
import fr.centralesupelec.gRPC.LogResponse;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogRequestService {
    private final Channel channel;

    public LogResponse getLog(String userId, String courseId) {
        containerServiceGrpc.containerServiceBlockingStub stub = containerServiceGrpc.newBlockingStub(channel);
        return stub.getLogs(
                LogRequest.newBuilder()
                        .setCourseID(courseId)
                        .setUserID(userId)
                        .build()
        );
    }
}
