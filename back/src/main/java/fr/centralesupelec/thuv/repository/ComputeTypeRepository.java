package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.model.ComputeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComputeTypeRepository extends JpaRepository<ComputeType, Long> {
    long countByTechnicalNameAndCluster(String technicalName, Cluster cluster);
    List<ComputeType> findByCluster(Cluster cluster);
}
