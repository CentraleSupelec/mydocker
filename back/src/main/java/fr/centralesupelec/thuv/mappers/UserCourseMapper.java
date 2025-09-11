package fr.centralesupelec.thuv.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import fr.centralesupelec.thuv.dtos.UserCourseDto;
import fr.centralesupelec.thuv.dtos.UserCourseWithSessionDto;
import fr.centralesupelec.thuv.model.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseMapper {
    private final PortsMapper portsMapper;
    private final ObjectMapper objectMapper;
    private final SessionMapper sessionMapper;

    public UserCourseDto convertToDto(Course course) {
        UserCourseDto dto = new UserCourseDto();
        applyToDto(course, dto);
        return dto;
    }

    public UserCourseWithSessionDto convertToDtoWihSession(Course course) {
        UserCourseWithSessionDto dto = new UserCourseWithSessionDto();
        applyToDto(course, dto);
        dto.setSessions(
                course.getSessions()
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
                .setTitle(course.getTitle())
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
