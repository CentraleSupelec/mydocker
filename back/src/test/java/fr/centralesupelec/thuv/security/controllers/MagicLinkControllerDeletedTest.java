package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.mail.EmailService;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.CourseStatus;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MagicLinkControllerDeletedTest {

    private static final String EMAIL = "first.last@example.com";
    private static final String COURSE_UUID = "7c2f2b1e-0000-4000-8000-000000000000";

    @Test
    void magicLinkRefusedForDeletedCourse() {
        MyUserDetailsService myUserDetailsService = mock(MyUserDetailsService.class);
        OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        UserCourseRepository userCourseRepository = mock(UserCourseRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        EmailService emailService = mock(EmailService.class);

        Course course = new Course();
        course.setId(42L);
        course.setStatus(CourseStatus.DELETED);
        course.setExternalAccess(true);
        course.setExternalAccessExpirationDate(LocalDateTime.now().plusDays(1));
        when(courseRepository.findByUuid(UUID.fromString(COURSE_UUID))).thenReturn(Optional.of(course));

        OneTimeToken consumedToken = mock(OneTimeToken.class);
        when(consumedToken.getUsername()).thenReturn(EMAIL + "/" + COURSE_UUID);
        when(oneTimeTokenService.consume(any(OneTimeTokenAuthenticationToken.class))).thenReturn(consumedToken);
        User user = new User();
        user.setId(7L);
        when(myUserDetailsService.findOrCreateMagicLinkUser(anyString())).thenReturn(user);

        MagicLinkController controller = new MagicLinkController(
                jwtTokenProvider,
                myUserDetailsService,
                mock(ActivityLogger.class),
                emailService,
                courseRepository,
                userCourseRepository,
                oneTimeTokenService
        );

        ResponseEntity<?> requested = controller.requestMagicLink(Map.of("email", EMAIL, "courseUuid", COURSE_UUID));
        assertEquals(401, requested.getStatusCode().value());
        verify(oneTimeTokenService, never()).generate(any(GenerateOneTimeTokenRequest.class));
        verifyNoInteractions(emailService);

        ResponseEntity<?> login = controller.magicLogin(Map.of("token", "a-valid-token"));
        assertEquals(401, login.getStatusCode().value());
        verify(userCourseRepository, never()).saveAndFlush(any());
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
    }
}
