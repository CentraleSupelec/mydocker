package fr.centralesupelec.thuv.storage;

import fr.centralesupelec.thuv.dtos.ShutdownContainerDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
public class ShutdownStatusStorage {
    private final HashMap<String, ShutdownContainerDto> containers;

    public ShutdownStatusStorage() {
        this.containers = new HashMap<>();
    }


    public void addContainer(String userId, String courseId) {
        String key = generateKey(userId, courseId);
        this.containers.put(key, new ShutdownContainerDto());
    }


    public Optional<ShutdownContainerDto> getContainer(String userId, String courseId) {
        String key = generateKey(userId, courseId);
        Optional<ShutdownContainerDto> optContainer = Optional.ofNullable(this.containers.getOrDefault(key, null));
        if (optContainer.isPresent() && (optContainer.get().getIsShutdown() || optContainer.get().getError() != null)) {
            this.containers.remove(key);
        }

        return optContainer;
    }

    private String generateKey(String userId, String courseId) {
        return userId + courseId;
    }
}
