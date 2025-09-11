package fr.centralesupelec.thuv.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Data
public class UserCourseDto {
    private Long id;
    private UUID uuid;
    private String title;
    private String description;
    private String creator;
    private List<PortDto> ports;
    private Boolean studentWorkIsSaved;
    private Boolean allowStudentToSubmit;
    private LocalDateTime createdAt;
    private LocalDateTime lastStartDate;
    private HashMap<String, Object> displayOptions = new HashMap<>();
    private int shutdownAfterMinutes = 0;
    private int warnShutdownMinutes = 0;
    private boolean externalAccess = false;
    private Long externalAccessExpirationDate;
}
