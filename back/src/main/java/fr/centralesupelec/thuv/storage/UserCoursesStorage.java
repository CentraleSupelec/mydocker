package fr.centralesupelec.thuv.storage;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserCoursesStorage {
    private final ConcurrentHashMap<String, List<String>> coursesByUserId;
    private LocalDateTime lastUpdate = null;

    public UserCoursesStorage() {
        this.coursesByUserId = new ConcurrentHashMap<>();
    }

    public void setAllCourses(Map<String, List<String>> newMap) {
        synchronized (this) {
            coursesByUserId.clear();
            newMap.forEach((userId, courseIds) -> 
                coursesByUserId.put(userId, new ArrayList<>(courseIds))
            );
            lastUpdate = LocalDateTime.now();
        }
    }

    public List<String> getCourses(String userId) {
        return coursesByUserId.getOrDefault(userId, Collections.emptyList());
    }

    public Map<String, List<String>> getAllCourses() {
        return coursesByUserId;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }
}
