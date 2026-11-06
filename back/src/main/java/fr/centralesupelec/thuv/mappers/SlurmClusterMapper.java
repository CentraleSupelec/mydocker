package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.SlurmClusterAdminDto;
import fr.centralesupelec.thuv.dtos.SlurmClusterDto;
import fr.centralesupelec.thuv.dtos.SlurmClusterView;
import fr.centralesupelec.thuv.model.SlurmCluster;
import org.springframework.stereotype.Component;

@Component
public class SlurmClusterMapper {
    public SlurmClusterDto convertToDto(SlurmClusterView slurmCluster) {
        SlurmClusterDto dto = new SlurmClusterDto();
        dto.setId(slurmCluster.getId());
        dto.setDisplayName(slurmCluster.getDisplayName());
        return dto;
    }

    public SlurmClusterAdminDto convertToAdminDto(SlurmCluster slurmCluster) {
        SlurmClusterAdminDto dto = new SlurmClusterAdminDto();
        dto.setId(slurmCluster.getId());
        dto.setDisplayName(slurmCluster.getDisplayName());
        dto.setTechnicalName(slurmCluster.getTechnicalName());
        dto.setApiEndpoint(slurmCluster.getApiEndpoint());
        return dto;
    }

    public SlurmClusterAdminDto convertToAdminDto(SlurmClusterView slurmCluster) {
        SlurmClusterAdminDto dto = new SlurmClusterAdminDto();
        dto.setId(slurmCluster.getId());
        dto.setDisplayName(slurmCluster.getDisplayName());
        dto.setTechnicalName(slurmCluster.getTechnicalName());
        dto.setApiEndpoint(slurmCluster.getApiEndpoint());
        return dto;
    }
}