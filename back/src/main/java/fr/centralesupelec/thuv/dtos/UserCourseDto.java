package fr.centralesupelec.thuv.dtos;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class UserCourseDto {
    private Long id;
    private String title;
    private String description;
    private String creator;
    private List<PortDto> ports;
    private Boolean studentWorkIsSaved;
    private Boolean allowStudentToSubmit;
    private HashMap<String, Object> displayOptions = new HashMap<>();
    private int shutdownAfterMinutes = 0;
    private int warnShutdownMinutes = 0;
}
