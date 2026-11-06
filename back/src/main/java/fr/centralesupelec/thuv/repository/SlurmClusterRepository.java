package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.dtos.SlurmClusterView;
import fr.centralesupelec.thuv.model.SlurmCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlurmClusterRepository extends JpaRepository<SlurmCluster, Long> {
    long countByTechnicalName(String technicalName);

    List<SlurmClusterView> findAllByOrderById();
}