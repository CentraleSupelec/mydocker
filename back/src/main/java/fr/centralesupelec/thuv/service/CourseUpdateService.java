package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import fr.centralesupelec.thuv.mappers.SessionMapper;
import fr.centralesupelec.thuv.mappers.UpdateCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CourseUpdateService {
    private final CourseRepository courseRepository;
    private final SessionMapper sessionMapper;
    private final UpdateCourseMapper updateCourseMapper;
    public void updateCourse(AdminUpdateCourseDto courseDto, Course course) {
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            validate(courseDto, course);
        }
        updateCourseMapper.updateChanges(course, courseDto);
        courseRepository.saveAndFlush(course);
    }

    private void validate(AdminUpdateCourseDto courseDto, Course course) {
        course.getSessions().forEach(courseSession -> {
            if (courseSession.getCleanDeployment() == null || courseSession.getLaunchDeployment() == null) {
                return;
            }
            Optional<SessionUpdateDto> optionalSessionUpdateDto = courseDto.getSessions().stream().filter(
                    sessionUpdateDto -> courseSession.getId().equals(sessionUpdateDto.getId())
            ).findFirst();
            if (optionalSessionUpdateDto.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Session %d can not be removed because it is linked to a deployment",
                                courseSession.getId()
                        )
                );
            }
            if (!sessionMapper.convertToUpdateDto(courseSession).equals(optionalSessionUpdateDto.get())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "Session %d can not be edited because it is linked to a deployment",
                                courseSession.getId()
                        )
                );
            }
        });
    }
}
