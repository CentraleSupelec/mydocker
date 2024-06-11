package fr.centralesupelec.thuv.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.dtos.AdminCourseDto;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import fr.centralesupelec.thuv.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCourseMapper {
    private final PortsMapper portsMapper;
    private final ZoneId zoneId;
    private final SessionMapper adminCourseSessionMapper;
    private final ObjectMapper objectMapper;

    public AdminCourseDto convertToDto(Course course) {
        AdminCourseDto courseDto = (AdminCourseDto) new AdminCourseDto()
                .setSessions(
                        course.getSessions()
                                .stream()
                                .map(adminCourseSessionMapper::convertToDto)
                                .sorted(Comparator.comparingLong(SessionUpdateDto::getStartDateTime))
                                .collect(Collectors.toList())
                )
                .setCreator(course.getCreator().getName() + " " + course.getCreator().getLastname())
                .setId(course.getId())
                .setUpdatedOn(
                        course.getUpdatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setCreatedOn(
                        course.getCreatedOn().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setLink(course.getLink())
                .setDockerImage(course.getDockerImage())
                .setNanoCpusLimit(course.getNanoCpusLimit())
                .setMemoryBytesLimit(course.getMemoryBytesLimit())
                .setCommand(course.getCommand())
                .setComputeTypeId(course.getComputeType().getId())
                .setSaveStudentWork(course.isSaveStudentWork())
                .setWorkdirSize(course.getWorkdirSize())
                .setWorkdirPath(course.getWorkdirPath())
                .setTitle(course.getTitle())
                .setDescription(course.getDescription())
                .setAllowStudentToSubmit(course.isAllowStudentToSubmit())
                .setStatus(course.getStatus())
                .setShutdownAfterMinutes(course.getShutdownAfterMinutes())
                .setWarnShutdownMinutes(course.getWarnShutdownMinutes())
                .setPorts(
                        course.getPorts()
                                .stream()
                                .map(portsMapper::convertToDTO)
                                .collect(Collectors.toList())
                );

        try {
            courseDto.setDisplayOptions(
                    objectMapper.readValue(
                            course.getDisplayOptions(),
                            new TypeReference<>() {}
                    )
            );
        } catch (JsonProcessingException | IllegalArgumentException e) {
            courseDto.setDisplayOptions(
                    new HashMap<>()
            );
        }
        return courseDto;
    }
}
