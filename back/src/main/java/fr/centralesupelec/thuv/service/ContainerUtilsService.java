package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
@Service
public class ContainerUtilsService {
    private final ZoneId zoneId;
    private static final Long SecondsInAMinute = 60L;

    public static String generateKey(String userId, String courseId) {
        return userId + "-" + courseId;
    }

    public Long computeDeletionTime(Course course, LocalDateTime sessionEndTime, Boolean destroyContainerAfterEndTime) {
        long now = Instant.now().getEpochSecond();
        long postponableDeletionTime = now
                + (course.getShutdownAfterMinutes()
                * ContainerUtilsService.SecondsInAMinute);
        long sessionDeletionTime = sessionEndTime.atZone(zoneId).toEpochSecond();
        if (
                destroyContainerAfterEndTime
                        && course.getShutdownAfterMinutes() == 0
        ) {
            return sessionDeletionTime;
        }
        if (
                !destroyContainerAfterEndTime
                        && course.getShutdownAfterMinutes() > 0
        ) {
            return postponableDeletionTime;
        }
        if (
                destroyContainerAfterEndTime
                        && course.getShutdownAfterMinutes() > 0
        ) {
            return Math.min(sessionDeletionTime, postponableDeletionTime);
        }
        return null;
    }
}
