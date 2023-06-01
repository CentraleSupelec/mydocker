package fr.centralesupelec.thuv.scale_up.repository;

import fr.centralesupelec.thuv.scale_up.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    List<Deployment> findByOrderByStartDateTimeDesc();
    List<Deployment> findByStartDateTimeBeforeAndStatusOrderByStartDateTimeDesc(
            @NotNull LocalDateTime startDateTime, @NotNull Deployment.Status status
    );
}
