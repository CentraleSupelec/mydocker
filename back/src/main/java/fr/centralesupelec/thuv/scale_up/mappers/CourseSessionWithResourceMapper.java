package fr.centralesupelec.thuv.scale_up.mappers;

import fr.centralesupelec.thuv.mappers.AdminCourseMapper;
import fr.centralesupelec.thuv.model.CourseSession;
import fr.centralesupelec.thuv.scale_up.dtos.CourseSessionWithResourceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
public class CourseSessionWithResourceMapper {
    private final RessourceDescriptionMapper ressourceDescriptionMapper;
    private final ZoneId zoneId;
    private final AdminCourseMapper adminCourseMapper;


    @Autowired
    public CourseSessionWithResourceMapper(
            RessourceDescriptionMapper ressourceDescriptionMapper,
            ZoneId zoneId,
            AdminCourseMapper adminCourseMapper
    ) {
        this.ressourceDescriptionMapper = ressourceDescriptionMapper;
        this.zoneId = zoneId;
        this.adminCourseMapper = adminCourseMapper;
    }

    public CourseSessionWithResourceDto convertToDto(CourseSession courseSession) {
        return (CourseSessionWithResourceDto) new CourseSessionWithResourceDto()
                .setCourse(
                        adminCourseMapper.convertToDto(
                                courseSession.getCourse()
                        )
                )
                .setResources(
                        courseSession.getCourseSessionOvhResources()
                            .stream()
                            .map(ressourceDescriptionMapper::convertToDto)
                            .collect(Collectors.toList())
                )
                .setId(
                        courseSession.getId()
                )
                .setStudentNumber(
                        courseSession.getStudentNumber()
                )
                .setStartDateTime(
                        courseSession.getStartDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setEndDateTime(
                        courseSession.getEndDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setBlockContainerCreationBeforeStartTime(
                        courseSession.getBlockContainerCreationBeforeStartTime()
                )
                .setDestroyContainerAfterEndTime(
                        courseSession.getDestroyContainerAfterEndTime()
                );
    }

    public void applyChangesForRessources(CourseSession courseSession, CourseSessionWithResourceDto dto) {
        courseSession.getCourseSessionOvhResources().clear();
        courseSession.getCourseSessionOvhResources().addAll(
                dto.getResources().stream()
                    .map(ressourceDescriptionMapper::convertToModel)
                    .peek(r -> r.setCourseSession(courseSession))
                    .collect(Collectors.toSet())
        );
    }
}
