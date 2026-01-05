package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.model.Cluster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ClusterMapper {

    public void updateCluster(ClusterUpdateDto dto, Cluster cluster) {
        cluster.setName(dto.getName())
            .setGoAddress(dto.getGoAddress())
            .setGoPort(dto.getGoPort())
            .setTerraformStateUrl(dto.getTerraformStateUrl())
            .setDeploymentEnabled(dto.getDeploymentEnabled())
        ;
    }
}
