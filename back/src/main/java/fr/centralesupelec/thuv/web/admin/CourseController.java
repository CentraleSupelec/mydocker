package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.AdminCourseDto;
import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.mappers.AdminCourseMapper;
import fr.centralesupelec.thuv.mappers.UpdateCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.service.CourseListService;
import fr.centralesupelec.thuv.service.CourseUpdateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@RestController("adminCourseController")
@RequestMapping("admin/courses")
@RequiredArgsConstructor
public class CourseController {
    private static final int LINK_LENGTH = 20;
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AdminCourseMapper adminCourseMapper;
    private final UpdateCourseMapper updateCourseMapper;
    private final CourseListService courseListService;
    private final CourseUpdateService courseUpdateService;

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping(value = "/")
    public Page<AdminCourseDto> getCourses(
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal,
            @RequestParam(value = "search") String search,
            @RequestParam(value = "status") @NotNull List<CourseStatus> status,
            final Pageable pageable
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );
        return courseListService.getViewableCourse(user, search, status, pageable)
                .map(adminCourseMapper::convertToDto);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public AdminCourseDto createCourse(
            @RequestBody @Valid AdminUpdateCourseDto dto,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal
    ) {
        User user = userRepository.getReferenceById(
                principal.getUserId()
        );

        Course course = new Course()
                .setCreator(user)
                .setLink(RandomStringUtils.randomAlphabetic(LINK_LENGTH));
        updateCourseMapper.updateChanges(course, dto);
        course = courseRepository.saveAndFlush(course);
        return adminCourseMapper.convertToDto(course);
    }

    @PreAuthorize("@coursePermissionService.canRead(#course)")
    @GetMapping(value = "/{courseId}")
    public AdminCourseDto getCourse(
            @PathVariable("courseId") Course course
    ) {
        return adminCourseMapper.convertToDto(course);
    }

    @PreAuthorize("@coursePermissionService.canEdit(#course)")
    @RequestMapping(value = "/{courseId}", method = RequestMethod.PUT)
    public ResponseEntity<AdminCourseDto> editCourse(
            @PathVariable("courseId") Course course,
            @RequestBody @Valid AdminUpdateCourseDto dto
    ) {
        courseUpdateService.updateCourse(dto, course);
        return ResponseEntity.ok().body(
                adminCourseMapper.convertToDto(course)
        );
    }

    @PreAuthorize("@coursePermissionService.canEdit(#course)")
    @RequestMapping(value = "/{courseId}", method = RequestMethod.DELETE)
    public void deleteCourse(
            @PathVariable("courseId") Course course
    ) {
        for (User s: course.getStudents()) {
            s.removeCourse(course);
            userRepository.save(s);
        }
        userRepository.flush();
        courseRepository.deleteById(course.getId());
    }
}
