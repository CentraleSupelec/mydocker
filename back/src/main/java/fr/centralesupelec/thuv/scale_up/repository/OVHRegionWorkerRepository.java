package fr.centralesupelec.thuv.scale_up.repository;

import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public interface OVHRegionWorkerRepository extends JpaRepository<OVHRegionWorker, Long> {
    List<OVHRegionWorker> findOVHRegionWorkerByLaunchDeploymentSessionsToLaunchAndCleanDeploymentIsNull(
            CourseSession launchDeploymentSession
    );
    List<OVHRegionWorker> findOVHRegionWorkerByLaunchDeploymentSessionsToLaunch(CourseSession launchDeploymentSession);
    List<OVHRegionWorker> findOVHRegionWorkerByLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentStartDateTimeAfter(
            @NotNull LocalDateTime launchDeploymentStartDateTime, @NotNull LocalDateTime cleanDeploymentStartDateTime
    );
    List<OVHRegionWorker> findOVHRegionWorkerByLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentIsNull(
            @NotNull LocalDateTime launchDeploymentStartDateTime
    );
    List<OVHRegionWorker> findOVHRegionWorkerByCleanDeploymentIsNullAndLaunchDeploymentStartDateTimeBefore(
            @NotNull LocalDateTime launchDeploymentStartDateTime
    );
}
