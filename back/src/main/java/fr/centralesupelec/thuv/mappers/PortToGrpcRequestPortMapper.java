package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.gRPC.RequestPort;
import fr.centralesupelec.thuv.model.Port;
import org.springframework.stereotype.Service;

@Service
public class PortToGrpcRequestPortMapper {
    public RequestPort mapToRequestPort(Port port) {
        return RequestPort.newBuilder()
                .setDescription(
                        port.getDescription()
                )
                .setConnexionType(
                        port.getConnectionType().toString()
                )
                .setPortToMap(
                        port.getMapPort()
                )
                .setRequiredToAccessContainer(
                        port.getRequiredToAccessContainer()
                )
                .build();
    }
}
