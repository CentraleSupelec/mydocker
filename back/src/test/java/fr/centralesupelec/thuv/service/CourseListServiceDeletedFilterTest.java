package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The status list comes straight from the client: the front only offers "Deleted" to
 * administrators, so the server has to refuse it to everyone else.
 */
class CourseListServiceDeletedFilterTest {
    private CourseRepository courseRepository;
    private CourseListService courseListService;
    private User user;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        courseListService = new CourseListService(courseRepository);
        user = new User();
        user.setId(1L);
    }

    @Test
    void nonAdminAskingForDeletedIsRefused() {
        List<List<CourseStatus>> statusLists = List.of(
                List.of(CourseStatus.DELETED),
                List.of(CourseStatus.READY, CourseStatus.DELETED)
        );
        for (List<CourseStatus> status : statusLists) {
            for (String search : List.of("", "abc")) {
                ResponseStatusException thrown = assertThrows(
                        ResponseStatusException.class,
                        () -> courseListService.getViewableCourse(user, false, search, status, Pageable.unpaged())
                );
                assertEquals(HttpStatus.FORBIDDEN.value(), thrown.getStatusCode().value());
                verifyNoInteractions(courseRepository);
            }
        }
    }

    @Test
    void adminAskingForDeletedIsServed() {
        Pageable pageable = Pageable.unpaged();
        courseListService.getViewableCourse(user, true, "abc", List.of(CourseStatus.DELETED), pageable);
        verify(courseRepository, times(1)).searchCoursesByTitleOrCreatorName("abc", List.of(CourseStatus.DELETED), pageable);

        reset(courseRepository);
        courseListService.getViewableCourse(
                user, false, "", List.of(CourseStatus.DRAFT, CourseStatus.TEST, CourseStatus.READY), pageable
        );
        verify(courseRepository, never()).searchCoursesByTitleOrCreatorName(any(), any(), any());
        verify(courseRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
