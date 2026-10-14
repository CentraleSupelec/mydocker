package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.mappers.ClusterMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
    private final ClusterRepository clusterRepository;
    private final ClusterMapper clusterMapper;

    public Cluster updateCluster(ClusterUpdateDto dto, Cluster cluster) {
        // The name keys the gRPC channels: two clusters with one name would share a connection.
        boolean nameTaken = cluster.getId() == null
                ? clusterRepository.existsByName(dto.getName())
                : clusterRepository.existsByNameAndIdNot(dto.getName(), cluster.getId());
        if (nameTaken) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("A cluster named %s already exists", dto.getName())
            );
        }
        clusterMapper.updateCluster(dto, cluster);
        this.clusterRepository.saveAndFlush(cluster);
        return cluster;
    }

    public void deleteCluster(Cluster cluster) {
        this.clusterRepository.delete(cluster);
    }
}
