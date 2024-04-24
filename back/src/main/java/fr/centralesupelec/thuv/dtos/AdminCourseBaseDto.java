package fr.centralesupelec.thuv.dtos;

import fr.centralesupelec.thuv.model.CourseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class AdminCourseBaseDto {
    @NotEmpty
    @Pattern(regexp = "^[^/]+$", message = "Title should not contain /")
    private String title;
    @NotEmpty
    private String description;
    @NotEmpty
    @Valid
    private List<PortDto> ports;
    @NotNull
    private CourseStatus status;

    @NotEmpty
    private String dockerImage;
    private Long nanoCpusLimit;
    private Long memoryBytesLimit;
    private String command;

    private int shutdownAfterMinutes = 0;
    private int warnShutdownMinutes = 0;

    @NotNull
    private Long computeTypeId;

    @NotNull
    private Boolean saveStudentWork = false;
    private Integer workdirSize;
    private String workdirPath;
    @NotNull
    private Boolean allowStudentToSubmit = false;

    @NotNull
    private HashMap<String, Object> displayOptions = new HashMap<>();
}
