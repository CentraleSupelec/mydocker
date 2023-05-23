package fr.centralesupelec.thuv.mappers;

import fr.centralesupelec.thuv.dtos.SessionWithCourseDto;
import fr.centralesupelec.thuv.model.CourseSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SessionWithCourseMapper {
    private final ZoneId zoneId;
    private final UserCourseMapper userCourseMapper;

    public SessionWithCourseDto convertToDto(CourseSession courseSession) {
        return (SessionWithCourseDto) new SessionWithCourseDto()
                .setCourse(
                        userCourseMapper.convertToDto(
                                courseSession.getCourse()
                        )
                )
                .setDestroyContainerAfterEndTime(
                        courseSession.getDestroyContainerAfterEndTime()
                )
                .setId(
                        courseSession.getId()
                )
                .setBlockContainerCreationBeforeStartTime(
                        courseSession.getBlockContainerCreationBeforeStartTime()
                )
                .setStartDateTime(
                        courseSession.getStartDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setEndDateTime(
                        courseSession.getEndDateTime().atZone(zoneId).toInstant().toEpochMilli()
                )
                .setStudentNumber(
                        courseSession.getStudentNumber()
                );
    }
}
