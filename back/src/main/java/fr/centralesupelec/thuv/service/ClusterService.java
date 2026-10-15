package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.ClusterChannels;
import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.mappers.ClusterMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClusterService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
    private final ClusterRepository clusterRepository;
    private final ClusterMapper clusterMapper;
    // A provider: the channel bean is lazy and needs the clusters table, so resolve it on use.
    private final ObjectProvider<ClusterChannels> channels;

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
        ClusterChannels running = channels.getObject();
        Long runningOwner = running.ownerOf(dto.getName());
        if (runningOwner != null && !runningOwner.equals(cluster.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format(
                            "The name %s still belongs to another cluster until the back end restarts",
                            dto.getName()
                    )
            );
        }
        String previousName = cluster.getName();
        boolean routingChanged = cluster.getId() != null && (
                !Objects.equals(cluster.getName(), dto.getName())
                        || !Objects.equals(cluster.getGoAddress(), dto.getGoAddress())
                        || cluster.getGoPort() != dto.getGoPort()
        );
        clusterMapper.updateCluster(dto, cluster);
        this.clusterRepository.saveAndFlush(cluster);
        if (routingChanged) {
            running.markStale(previousName);
            logger.info("Cluster {} changed its gRPC endpoint or name: it is unreachable until the back end restarts", previousName);
        }
        return cluster;
    }

    public void deleteCluster(Cluster cluster) {
        this.clusterRepository.delete(cluster);
        channels.getObject().markStale(cluster.getName());
    }
}
