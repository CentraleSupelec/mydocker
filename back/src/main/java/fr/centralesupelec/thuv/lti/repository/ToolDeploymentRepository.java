package fr.centralesupelec.thuv.lti.repository;

import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface ToolDeploymentRepository extends JpaRepository<ToolDeployment, Long> {
    Optional<ToolDeployment> findByIss(String iss);

    Optional<ToolDeployment> findByClientId(String clientId);
}
