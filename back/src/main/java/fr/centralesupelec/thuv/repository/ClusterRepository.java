package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.dtos.ClusterBasicView;
import fr.centralesupelec.thuv.model.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    List<ClusterBasicView> findAllByOrderById();
}
