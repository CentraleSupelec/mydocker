package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.mappers.ClusterMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
    private final ClusterRepository clusterRepository;
    private final ClusterMapper clusterMapper;

    public Cluster updateCluster(ClusterUpdateDto dto, Cluster cluster) {
        clusterMapper.updateCluster(dto, cluster);
        this.clusterRepository.saveAndFlush(cluster);
        return cluster;
    }

    public void deleteCluster(Cluster cluster) {
        this.clusterRepository.delete(cluster);
    }
}
