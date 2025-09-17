package fr.centralesupelec.thuv.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.activity_logging.model.ActivityLogRecord;
import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.model.LogModelName;
import fr.centralesupelec.thuv.activity_logging.repository.LogRecordRepository;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import fr.centralesupelec.thuv.dtos.UserCourseDto;
import fr.centralesupelec.thuv.dtos.UserCourseWithSessionDto;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.UserCourse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseMapper {
    private final PortsMapper portsMapper;
    private final ObjectMapper objectMapper;
    private final SessionMapper sessionMapper;
    private final ZoneId zoneId;
    private final LogRecordRepository logRecordRepository;

    @Value("${app.ignore_recent_last_start_date_in_seconds}")
    private Long ignoreRecentLastStartDateInSeconds;

    public UserCourseDto convertToDto(Course course) {
        UserCourseDto dto = new UserCourseDto();
        applyToDto(course, dto);
        return dto;
    }

    public UserCourseWithSessionDto convertToDtoWihSession(UserCourse userCourse) {
        UserCourseWithSessionDto dto = new UserCourseWithSessionDto();
        dto.setCreatedAt(userCourse.getCreatedAt());

        if (userCourse.getLastStartDate().isAfter(LocalDateTime.now(zoneId).minusSeconds(ignoreRecentLastStartDateInSeconds))) {
            Optional<ActivityLogRecord> latestLogRecordOptional = logRecordRepository
                .findFirstByUserIdAndModelIdAndModelNameAndActionInAndCreatedOnBeforeOrderByCreatedOnDesc(
                    userCourse.getUser().getId(),
                    Long.toString(userCourse.getCourse().getId()),
                    LogModelName.COURSE,
                    List.of(LogAction.ENVIRONMENT_ASK, LogAction.ENVIRONMENT_RESTART),
                    LocalDateTime.now().minusSeconds(ignoreRecentLastStartDateInSeconds)
            );
            latestLogRecordOptional.ifPresentOrElse(
                latestLogRecord -> 
                    dto.setLastStartDate(latestLogRecord.getCreatedOn().atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId).toLocalDateTime()),
                () -> dto.setLastStartDate(userCourse.getLastStartDate())
            );
        } else {
            dto.setLastStartDate(userCourse.getLastStartDate());
        }

        applyToDto(userCourse.getCourse(), dto);
        dto.setSessions(
                userCourse.getCourse().getSessions()
                    .stream()
                    .map(sessionMapper::convertToUpdateDto)
                    .sorted(Comparator.comparingLong(SessionUpdateDto::getStartDateTime))
                    .collect(Collectors.toList())
        );
        return dto;
    }

    private void applyToDto(Course course, UserCourseDto dto) {
        dto
                .setStudentWorkIsSaved(course.isSaveStudentWork())
                .setId(course.getId())
                .setUuid(course.getUuid())
                .setTitle(course.getTitle())
                .setExternalAccess(course.isExternalAccess())
                .setExternalAccessExpirationDate(
                    course.getExternalAccessExpirationDate() == null
                        ? null
                        : course.getExternalAccessExpirationDate().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setDescription(course.getDescription())
                .setCreator(course.getCreator().getLastname())
                .setAllowStudentToSubmit(
                        course.isAllowStudentToSubmit()
                )
                .setWarnShutdownMinutes(course.getWarnShutdownMinutes())
                .setShutdownAfterMinutes(course.getShutdownAfterMinutes())
                .setPorts(
                        course.getPorts()
                                .stream()
                                .map(portsMapper::convertToDTO)
                                .collect(Collectors.toList())
                );

        try {
            dto.setDisplayOptions(
                    objectMapper.readValue(
                            course.getDisplayOptions(), new TypeReference<>() {}
                    )
            );
        } catch (JsonProcessingException e) {
            dto.setDisplayOptions(
                    new HashMap<>()
            );
        }
    }
}
