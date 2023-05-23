package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.PortDto;
import fr.centralesupelec.thuv.model.ConnectionType;
import fr.centralesupelec.thuv.model.Port;
import org.springframework.stereotype.Service;

@Service
public class PortsMapper {
    public PortDto convertToDTO(Port port) {
        return new PortDto()
                .setMapPort(port.getMapPort())
                .setDescription(port.getDescription())
                .setConnectionType(port.getConnectionType().toString())
                .setRequiredToAccessContainer(port.getRequiredToAccessContainer());
    }

    public Port convertDTOToModel(PortDto portDTO) {
        return new Port()
                .setMapPort(portDTO.getMapPort())
                .setDescription(portDTO.getDescription())
                .setConnectionType(ConnectionType.valueOf(portDTO.getConnectionType()))
                .setRequiredToAccessContainer(portDTO.getRequiredToAccessContainer());
    }
}
