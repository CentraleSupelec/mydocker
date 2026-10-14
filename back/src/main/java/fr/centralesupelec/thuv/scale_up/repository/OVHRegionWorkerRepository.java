package fr.centralesupelec.thuv.scale_up.repository;

import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.scale_up.model.OVHRegionWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query("""
        SELECT DISTINCT w FROM OVHRegionWorker w
        JOIN FETCH w.launchDeployment ld
        LEFT JOIN FETCH w.cleanDeployment cd
        LEFT JOIN FETCH ld.sessionsToLaunch s
        LEFT JOIN FETCH ld.workersToLaunch
        LEFT JOIN FETCH s.course c
        WHERE ld.startDateTime <= :launchDeploymentStartDateTime 
                AND (w.cleanDeployment IS NULL OR cd.startDateTime >= :cleanDeploymentStartDateTime)
    """)
    List<OVHRegionWorker> findOVHRegionWorkerByLaunchDeploymentStartDateTimeBeforeAndCleanDeploymentStartDateTimeAfterOrCleanDeploymentIsNull(
        @Param("launchDeploymentStartDateTime") LocalDateTime launchDeploymentStartDateTime,
        @Param("cleanDeploymentStartDateTime") LocalDateTime cleanDeploymentStartDateTime
    );
}
