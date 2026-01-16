package fr.centralesupelec.thuv.storage;

import fr.centralesupelec.thuv.dtos.ShutdownContainerDto;
import fr.centralesupelec.thuv.service.ContainerUtilsService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Service
public class ShutdownStatusStorage {
    private final ConcurrentHashMap<String, ShutdownContainerDto> containers;

    public ShutdownStatusStorage() {
        this.containers = new ConcurrentHashMap<>();
    }


    public void addContainer(String userId, String courseId) {
        String key = ContainerUtilsService.generateKey(userId, courseId);
        this.containers.put(key, new ShutdownContainerDto());
    }


    public Optional<ShutdownContainerDto> getContainer(String userId, String courseId) {
        String key = ContainerUtilsService.generateKey(userId, courseId);
        Optional<ShutdownContainerDto> optContainer = Optional.ofNullable(this.containers.getOrDefault(key, null));
        if (optContainer.isPresent() && (optContainer.get().getIsShutdown() || optContainer.get().getError() != null)) {
            this.containers.remove(key);
        }

        return optContainer;
    }
}
