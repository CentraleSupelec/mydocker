package fr.centralesupelec.thuv.scale_up.repository;

import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeploymentStatusRepository extends JpaRepository<DeploymentStatus, Long> {
    List<DeploymentStatus> findByCreatedOnBefore(LocalDateTime createdOn);
    Page<DeploymentStatus> findByOrderByCreatedOnDesc(Pageable pageable);
    Optional<DeploymentStatus> findByStatusInOrderByUpdatedOnDesc(List<DeploymentStatus.Status> status);
}
