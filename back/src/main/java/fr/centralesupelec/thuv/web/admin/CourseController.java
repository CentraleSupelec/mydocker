package fr.centralesupelec.thuv.web.admin;

import fr.centralesupelec.thuv.dtos.AdminCourseDto;
import fr.centralesupelec.thuv.dtos.AdminUpdateCourseDto;
import fr.centralesupelec.thuv.mappers.AdminCourseMapper;
import fr.centralesupelec.thuv.mappers.UpdateCourseMapper;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import fr.centralesupelec.thuv.service.ConnectedUsersByCourseIdMapService;
import fr.centralesupelec.thuv.service.CourseListService;
import fr.centralesupelec.thuv.service.CourseUpdateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("adminCourseController")
@RequestMapping("admin/courses")
@RequiredArgsConstructor
public class CourseController {
    private static final int LINK_LENGTH = 20;
    private static final int DEFAULT_NUMBER_OF_DAYS_FOR_RECENT_USERS = 90;
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final AdminCourseMapper adminCourseMapper;
    private final UpdateCourseMapper updateCourseMapper;
    private final CourseListService courseListService;
    private final CourseUpdateService courseUpdateService;
    private final ConnectedUsersByCourseIdMapService connectedUsersByCourseIdMapService;
    private final ZoneId zoneId;

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping(value = "/")
    public Page<AdminCourseDto> getCourses(
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal,
            @RequestParam(value = "search") String search,
            @RequestParam(value = "status") @NotNull List<CourseStatus> status,
            @RequestParam(value = "dateLimit", required = false) Long dateLimit,
            final Pageable pageable) {
        String decodedSearch = URLDecoder.decode(search, StandardCharsets.UTF_8);
        User user = userRepository.getReferenceById(
                principal.getUserId());

        Map<String, Integer> connectedUsersByCourseIdMap = connectedUsersByCourseIdMapService.getConnectedUsers();

        LocalDateTime dateLimitLocalDateTime = LocalDateTime.now().minusDays(DEFAULT_NUMBER_OF_DAYS_FOR_RECENT_USERS);

        if (dateLimit != null) {
            dateLimitLocalDateTime = Instant.ofEpochMilli(dateLimit).atZone(zoneId).toLocalDateTime();
        }

        Map<Long, Integer> recentUsersByCourseId = userCourseRepository.countRecentUsersByCourse(dateLimitLocalDateTime)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> ((Long) r[1]).intValue()));

        List<String> computedFields = List.of("numberOfConnectedUsers", "numberOfRecentUsers");

        boolean sortByComputedField = pageable.getSort().stream()
            .anyMatch(order -> computedFields.contains(order.getProperty()));

        if (sortByComputedField) {
            List<AdminCourseDto> dtos = courseListService.getViewableCourse(user, decodedSearch, status, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .map(course -> {
                        int connectedCount = connectedUsersByCourseIdMap.getOrDefault(course.getId().toString(), 0);
                        int recentUsers = recentUsersByCourseId.getOrDefault(course.getId(), 0);
                        return adminCourseMapper.convertToDto(course, connectedCount, recentUsers);
                    })
                    .sorted((dto1, dto2) -> {
                        for (Sort.Order order : pageable.getSort()) {
                            String property = order.getProperty();
                            if (computedFields.contains(property)) {
                                try {
                                    Method getter = AdminCourseDto.class.getMethod(
                                            "get" + property.substring(0, 1).toUpperCase() + property.substring(1)
                                    );
                                    Integer value1 = (Integer) getter.invoke(dto1);
                                    Integer value2 = (Integer) getter.invoke(dto2);

                                    int cmp = Integer.compare(value1, value2);
                                    if (cmp != 0) {
                                        return order.getDirection() == Sort.Direction.ASC ? cmp : -cmp;
                                    }
                                } catch (ReflectiveOperationException e) {
                                    throw new RuntimeException("Failed to access getter for computed field: " + property, e);
                                }
                            }
                        }
                        return 0;
                    })
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), dtos.size());
            List<AdminCourseDto> pageContent = dtos.subList(start, end);

            return new PageImpl<>(pageContent, pageable, dtos.size());
        }

        return courseListService.getViewableCourse(user, decodedSearch, status, pageable)
                .map(course -> {
                    int connectedCount = connectedUsersByCourseIdMap.getOrDefault(course.getId().toString(), 0);
                    int recentUsers = recentUsersByCourseId.getOrDefault(course.getId(), 0);
                    return adminCourseMapper.convertToDto(course, connectedCount, recentUsers);
                });
    }

    @PreAuthorize("hasRole('TEACHER')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public AdminCourseDto createCourse(
            @RequestBody @Valid AdminUpdateCourseDto dto,
            @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails principal) {
        User user = userRepository.getReferenceById(
                principal.getUserId());

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
            @PathVariable("courseId") Course course) {
        return adminCourseMapper.convertToDto(course);
    }

    @PreAuthorize("@coursePermissionService.canEdit(#course)")
    @RequestMapping(value = "/{courseId}", method = RequestMethod.PUT)
    public ResponseEntity<AdminCourseDto> editCourse(
            @PathVariable("courseId") Course course,
            @RequestBody @Valid AdminUpdateCourseDto dto) {
        courseUpdateService.updateCourse(dto, course);
        return ResponseEntity.ok().body(
                adminCourseMapper.convertToDto(course));
    }

    @PreAuthorize("@coursePermissionService.canEdit(#course)")
    @RequestMapping(value = "/{courseId}", method = RequestMethod.DELETE)
    public void deleteCourse(
            @PathVariable("courseId") Course course) {
        for (User s : course.getStudents()) {
            s.removeCourse(course);
            userRepository.save(s);
        }
        userRepository.flush();
        courseRepository.deleteById(course.getId());
    }
}
