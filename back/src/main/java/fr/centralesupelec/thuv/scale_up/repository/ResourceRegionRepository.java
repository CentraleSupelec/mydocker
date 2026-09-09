package fr.centralesupelec.thuv.scale_up.repository;

import fr.centralesupelec.thuv.model.ComputeType;
import fr.centralesupelec.thuv.model.ResourceRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHRegion;
import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ResourceRegionRepository extends JpaRepository<ResourceRegion, String> {
    Optional<ResourceRegion> findByComputeTypeAndResourceAndRegion(ComputeType computeType, OVHResource ovhResource, OVHRegion ovhRegion);
}
