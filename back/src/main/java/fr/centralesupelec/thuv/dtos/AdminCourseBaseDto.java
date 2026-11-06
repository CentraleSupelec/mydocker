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
    // Docker reference grammar: optional registry host and port, lowercase path, optional tag, optional digest.
    @Pattern(regexp = "^(?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])"
            + "(?:\\.(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]))*(?::[0-9]+)?/)?"
            + "[a-z0-9]+(?:(?:[._]|__|-+)[a-z0-9]+)*(?:/[a-z0-9]+(?:(?:[._]|__|-+)[a-z0-9]+)*)*"
            + "(?::[\\w][\\w.-]{0,127})?"
            + "(?:@[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*:[0-9a-fA-F]{32,})?$",
            message = "Docker image reference is malformed: no spaces, lowercase path")
    private String dockerImage;
    private Long nanoCpusLimit;
    private Long memoryBytesLimit;
    @NotNull
    private String command;

    private int shutdownAfterMinutes = 0;
    private int warnShutdownMinutes = 0;

    @NotNull
    private Long computeTypeId;

    private Long slurmClusterId;

    @NotNull
    private Boolean saveStudentWork = false;
    private Integer workdirSize;
    private String workdirPath;
    @NotNull
    private Boolean useStudentVolume = false;
    private String studentVolumePath;
    @NotNull
    private Boolean allowStudentToSubmit = false;

    @NotNull
    private Boolean visible = false;

    @NotNull
    private HashMap<String, Object> displayOptions = new HashMap<>();

    @NotNull
    private boolean externalAccess = false;

    private Long externalAccessExpirationDate;
    private String uid;
    private Long numberOfUsers;
    private int numberOfConnectedUsers;
    private int numberOfRecentUsers;
}
