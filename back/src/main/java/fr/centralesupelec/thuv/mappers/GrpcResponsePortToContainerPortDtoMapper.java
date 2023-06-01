package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.gRPC.ResponsePort;
import fr.centralesupelec.thuv.dtos.ContainerPortDto;
import org.springframework.stereotype.Service;

@Service
public class GrpcResponsePortToContainerPortDtoMapper {
    public ContainerPortDto convertToContainerPortDto(ResponsePort responsePort) {
        return (ContainerPortDto) new ContainerPortDto()
                .setPortMapTo(
                        responsePort.getMapTo()
                )
                .setDescription(
                        responsePort.getDescription()
                )
                .setMapPort(
                        responsePort.getPortToMap()
                )
                .setConnectionType(
                        responsePort.getConnexionType()
                )
                .setRequiredToAccessContainer(
                        responsePort.getRequiredToAccessContainer()
                );
    }
}
