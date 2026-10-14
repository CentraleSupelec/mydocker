package fr.centralesupelec.thuv.service;

import fr.centralesupelec.gRPC.CourseIds;
import fr.centralesupelec.gRPC.containerServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.google.protobuf.Empty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseIdsByUserService {
    private final ManagedChannel channel;
    public Map<String, List<String>> getCourseIdsByUser() {
        containerServiceGrpc.containerServiceBlockingStub stub =
            containerServiceGrpc.newBlockingStub(channel);
    
        Map<String, List<String>> result = new HashMap<>();
    
        Map<String, CourseIds> courseIdsByUserIdMap =
            stub.getCourseIdsByUser(Empty.getDefaultInstance())
                .getCourseIdsByUserIdMapMap();
    
        for (Map.Entry<String, CourseIds> entry : courseIdsByUserIdMap.entrySet()) {
            result.put(
                entry.getKey(),
                new ArrayList<>(entry.getValue().getCourseIdsList())
            );
        }
    
        return result;
    }
}
