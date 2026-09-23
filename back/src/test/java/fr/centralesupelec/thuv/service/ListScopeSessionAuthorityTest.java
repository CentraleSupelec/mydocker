package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.docker_build.repository.DockerImageRepository;
import fr.centralesupelec.thuv.docker_build.service.DockerImageListService;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The listing branch is chosen by what the session may do, not by what the account holds in the
 * database. An account can be an administrator and still be using a session opened through a
 * provider that does not grant administrator authority; the unrestricted listing must not be
 * reachable that way.
 */
class ListScopeSessionAuthorityTest {
    private CourseRepository courseRepository;
    private DockerImageRepository dockerImageRepository;
    private CourseListService courseListService;
    private DockerImageListService dockerImageListService;
    private User administratorInTheDatabase;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        dockerImageRepository = mock(DockerImageRepository.class);
        courseListService = new CourseListService(courseRepository);
        dockerImageListService = new DockerImageListService(dockerImageRepository);

        Role admin = new Role();
        admin.setName("ROLE_ADMIN");
        Role teacher = new Role();
        teacher.setName("ROLE_TEACHER");
        administratorInTheDatabase = new User();
        administratorInTheDatabase.setId(1L);
        administratorInTheDatabase.setRoles(new ArrayList<>(List.of(admin, teacher)));
    }

    @Test
    void aSessionWithoutAdminAuthorityGetsTheRestrictedCourseListing() {
        courseListService.getViewableCourse(
                administratorInTheDatabase, false, "", List.of(CourseStatus.READY), Pageable.unpaged()
        );

        verify(courseRepository, never()).searchCoursesByTitleOrCreatorName(any(), any(), any());
        verify(courseRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void aSessionWithAdminAuthorityGetsTheFullCourseListing() {
        courseListService.getViewableCourse(
                administratorInTheDatabase, true, "", List.of(CourseStatus.READY), Pageable.unpaged()
        );

        verify(courseRepository).searchCoursesByTitleOrCreatorName(any(), any(), any());
        verify(courseRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void aSessionWithoutAdminAuthorityGetsTheRestrictedImageListing() {
        dockerImageListService.getViewableDockerImage(administratorInTheDatabase, false, null, Pageable.unpaged());

        verify(dockerImageRepository, never()).findAllBy(any(Pageable.class));
        verify(dockerImageRepository, never()).findAllByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        verify(dockerImageRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void aSessionWithAdminAuthorityGetsTheFullImageListing() {
        dockerImageListService.getViewableDockerImage(administratorInTheDatabase, true, null, Pageable.unpaged());

        verify(dockerImageRepository).findAllBy(any(Pageable.class));
        verify(dockerImageRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
