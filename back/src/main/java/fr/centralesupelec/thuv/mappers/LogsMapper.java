package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.gRPC.LogResponse;
import fr.centralesupelec.thuv.docker_build.dtos.LogResponseDto;
import org.springframework.stereotype.Service;

@Service
public class LogsMapper {
    public LogResponseDto convertToDTO(LogResponse logResponse) {
        return new LogResponseDto()
                .setName(logResponse.getName())
                .setImage(logResponse.getImage())
                .setLogsByNode(logResponse.getLogsMap());
    }
}
