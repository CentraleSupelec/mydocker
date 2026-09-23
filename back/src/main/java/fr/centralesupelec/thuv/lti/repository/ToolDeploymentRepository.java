package fr.centralesupelec.thuv.lti.repository;

import fr.centralesupelec.thuv.lti.model.ToolDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface ToolDeploymentRepository extends JpaRepository<ToolDeployment, Long> {
    /**
     * A deployment is identified by the pair, not by either half. One integration is registered per
     * espace de cours, each with its own client id, so an issuer legitimately carries many rows; and
     * a client id is chosen by the platform that issues it, so it is unique only within an issuer.
     */
    Optional<ToolDeployment> findByIssAndClientId(String iss, String clientId);

    List<ToolDeployment> findAllByIss(String iss);

    Optional<ToolDeployment> findByIss(String iss);

    Optional<ToolDeployment> findByClientId(String clientId);
}
