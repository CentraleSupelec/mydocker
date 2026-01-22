package fr.centralesupelec.thuv.storage;

import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.dtos.ContainerDto;
import fr.centralesupelec.thuv.dtos.ContainerStatusDto;
import fr.centralesupelec.thuv.dtos.ContainerSwarmStateDto;
import fr.centralesupelec.thuv.service.ContainerUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Optional;

@Service
public class ContainerStorage {
    private static final Logger logger = LoggerFactory.getLogger(ContainerStorage.class);
    private final ConcurrentHashMap<String, ContainerDto> containers;
    // Use distinct HashMaps to avoid concurrency writings to state/status
    private final ConcurrentHashMap<String, ContainerSwarmStateDto> containersStates;
    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();
    private final ActivityLogger activityLogger;

    private static class LockEntry {
        final ReentrantLock lock = new ReentrantLock();
        int count = 0;
    }

    public ContainerStorage(ActivityLogger activityLogger) {
        this.containers = new ConcurrentHashMap<>();
        this.containersStates = new ConcurrentHashMap<>();
        this.activityLogger = activityLogger;
    }


    public void addContainer(ContainerDto containerDto, String userId, String courseId) {
        String key = ContainerUtilsService.generateKey(userId, courseId);
        logger.debug("Adding container {} to storage: {}", key, containerDto);
        this.containers.put(key, containerDto);
        LogAction logAction = switch (containerDto.getStatus()) {
            case OK -> LogAction.ENVIRONMENT_CREATED_OK;
            case KO -> LogAction.ENVIRONMENT_CREATED_KO;
            case PENDING -> LogAction.ENVIRONMENT_PENDING;
            case CHECKING -> LogAction.ENVIRONMENT_CHECKING;
        };
        activityLogger.log(
                logAction,
                LogModelName.COURSE,
                courseId,
                userId
        );
    }

    public void setContainerState(String userId, String courseId, ContainerSwarmStateDto state) {
        String key = ContainerUtilsService.generateKey(userId, courseId);
        this.containersStates.put(key, state);
    }

    public void addAdminContainer(ContainerDto containerDto, Long courseId) {
        String key = generateAdminKey(String.valueOf(courseId));
        this.containers.put(key, containerDto);
    }

    public Optional<ContainerDto> getContainer(String userId, String courseId) {
        return this.getContainer(userId, courseId, true);
    }

    public Optional<ContainerDto> getContainer(String userId, String courseId, Boolean removeIfNeeded) {

        String key = ContainerUtilsService.generateKey(userId, courseId);
        Optional<ContainerDto> optContainer = Optional.ofNullable(this.containers.getOrDefault(key, null));
        optContainer.ifPresent(containerDto -> {
            containerDto.setState(this.containersStates.get(key));
            if (removeIfNeeded
                && (
                containerDto.getStatus().equals(ContainerStatusDto.OK)
                    || containerDto.getStatus().equals(ContainerStatusDto.KO)
            )) {
                logger.debug("Removing container {} from storage", key);
                this.containers.remove(key);
                this.containersStates.remove(key);
            }
        });

        return optContainer;
    }


    public Optional<ContainerDto> getAdminContainer(Long courseId) {
        String key = generateAdminKey(String.valueOf(courseId));
        return Optional.ofNullable(this.containers.getOrDefault(key, null));
    }

    private String generateAdminKey(String courseId) {
        return String.format("%s-admin", courseId);
    }

    public void lock(String key) {
        locks.compute(key, (k, v) -> {
            if (v == null) {
                v = new LockEntry();
            }
            v.count++;
            return v;
        }).lock.lock();
    }

    public void unlock(String key) {
        locks.compute(key, (k, v) -> {
            if (v == null) {
                return null;
            }

            v.lock.unlock();
            v.count--;

            return (v.count == 0) ? null : v;
        });
    }
}
