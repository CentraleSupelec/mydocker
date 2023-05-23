package fr.centralesupelec.thuv.scale_up.repository;

import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OVHResourceRepository extends JpaRepository<OVHResource, Long> {
    Optional<OVHResource> findByType(String type);
}
