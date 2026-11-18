package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.dtos.ClusterBasicView;
import fr.centralesupelec.thuv.model.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    List<ClusterBasicView> findAllByOrderById();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Cluster> findByName(String name);
}
