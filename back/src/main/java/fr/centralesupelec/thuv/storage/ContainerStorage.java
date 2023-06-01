package fr.centralesupelec.thuv.storage;

import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
public class ContainerStorage {
    private final HashMap<String, ContainerDto> containers;
    private final ActivityLogger activityLogger;

    public ContainerStorage(ActivityLogger activityLogger) {
        this.containers = new HashMap<>();
        this.activityLogger = activityLogger;
    }


    public void addContainer(ContainerDto containerDto, String userId, String courseId) {
        String key = generateKey(userId, courseId);
        this.containers.put(key, containerDto);
        activityLogger.log(
                containerDto.getStatus().equals(ContainerStatusDto.OK)
                        ? LogAction.ENVIRONMENT_CREATED_OK
                        : LogAction.ENVIRONMENT_CREATED_KO,
                LogModelName.COURSE,
                courseId,
                userId
        );
    }

    public void addAdminContainer(ContainerDto containerDto, Long courseId) {
        String key = generateAdminKey(String.valueOf(courseId));
        this.containers.put(key, containerDto);
    }

    public Optional<ContainerDto> getContainer(String userId, String courseId) {
        String key = generateKey(userId, courseId);
        Optional<ContainerDto> optContainer = Optional.ofNullable(this.containers.getOrDefault(key, null));
        optContainer.ifPresent(theContainer -> this.containers.remove(key));

        return optContainer;
    }

    public Optional<ContainerDto> getAdminContainer(Long courseId) {
        String key = generateAdminKey(String.valueOf(courseId));
        return Optional.ofNullable(this.containers.getOrDefault(key, null));
    }

    private String generateKey(String userId, String courseId) {
        return userId + courseId;
    }
    private String generateAdminKey(String courseId) {
        return String.format("%s-admin", courseId);
    }
}
