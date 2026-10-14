package fr.centralesupelec.thuv.repository;

import fr.centralesupelec.thuv.model.ComputeType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComputeTypeRepository extends JpaRepository<ComputeType, Long> {
    long countByTechnicalName(String technicalName);
}
