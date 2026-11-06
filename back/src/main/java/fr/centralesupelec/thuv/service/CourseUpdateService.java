package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.dtos.SessionUpdateDto;
import fr.centralesupelec.thuv.mappers.SessionMapper;
import fr.centralesupelec.thuv.mappers.UpdateCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CourseUpdateService {
    private final CourseRepository courseRepository;
    private final SessionMapper sessionMapper;
    private final UpdateCourseMapper updateCourseMapper;
    public void updateCourse(AdminUpdateCourseDto courseDto, Course course) {
        // a course becomes DELETED only through markDeleted; an already deleted course may keep the status
        if (courseDto.getStatus() == CourseStatus.DELETED && course.getStatus() != CourseStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A course is deleted only through mark-deleted");
        }
        MyUserDetails principal = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            validate(courseDto, course);
            // only an admin binds a Slurm cluster; a teacher's edit must carry the current binding unchanged
            Long currentSlurmClusterId = course.getSlurmCluster() != null ? course.getSlurmCluster().getId() : null;
            if (!Objects.equals(currentSlurmClusterId, courseDto.getSlurmClusterId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an admin can change the Slurm cluster of a course");
            }
        }
        updateCourseMapper.updateChanges(course, courseDto);
        courseRepository.saveAndFlush(course);
    }

    /**
     * Only the status changes: sessions, enrolments, permissions and running containers stay as they are.
     */
    public void markDeleted(Course course) {
        course.setStatus(CourseStatus.DELETED);
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
