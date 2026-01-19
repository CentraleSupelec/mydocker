package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.model.CourseSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

@RequiredArgsConstructor
@Service
public class ContainerUtilsService {
    private final ZoneId zoneId;
    private static final Long SecondsInAMinute = 60L;

    public Long computeDeletionTime(CourseSession courseSession) {
        long now = Instant.now().getEpochSecond();
        long postponableDeletionTime = now
                + (courseSession.getCourse().getShutdownAfterMinutes()
                * ContainerUtilsService.SecondsInAMinute);
        long sessionDeletionTime = courseSession.getEndDateTime().atZone(zoneId).toEpochSecond();
        if (
                courseSession.getDestroyContainerAfterEndTime()
                        && courseSession.getCourse().getShutdownAfterMinutes() == 0
        ) {
            return sessionDeletionTime;
        }
        if (
                !courseSession.getDestroyContainerAfterEndTime()
                        && courseSession.getCourse().getShutdownAfterMinutes() > 0
        ) {
            return postponableDeletionTime;
        }
        if (
                courseSession.getDestroyContainerAfterEndTime()
                        && courseSession.getCourse().getShutdownAfterMinutes() > 0
        ) {
            return Math.min(sessionDeletionTime, postponableDeletionTime);
        }
        return null;
    }
}
