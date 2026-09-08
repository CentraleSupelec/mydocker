package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.exception.UserUpsertException;
import fr.centralesupelec.thuv.mail.EmailService;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MagicLinkControllerAmbiguousAccountTest {

    private static final String EMAIL = "first.last@example.com";
    private static final String COURSE_UUID = "7c2f2b1e-0000-4000-8000-000000000000";

    @Test
    void magicLoginLetsAnAmbiguousAccountReachTheExceptionHandler() {
        // Regression guard for the June incident: the catch-all in this method used to answer
        // "Invalid or expired magic link" for a token that was neither invalid nor expired, which
        // sent diagnosis to the token service instead of to the duplicate rows. The exception has
        // to leave the controller so AuthenticationExceptionHandler can answer 409.
        MyUserDetailsService myUserDetailsService = mock(MyUserDetailsService.class);
        OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        UserCourseRepository userCourseRepository = mock(UserCourseRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        ActivityLogger activityLogger = mock(ActivityLogger.class);

        OneTimeToken consumedToken = mock(OneTimeToken.class);
        when(consumedToken.getUsername()).thenReturn(EMAIL + "/" + COURSE_UUID);
        when(oneTimeTokenService.consume(any(OneTimeTokenAuthenticationToken.class))).thenReturn(consumedToken);
        when(myUserDetailsService.findOrCreateMagicLinkUser(anyString()))
                .thenThrow(new UserUpsertException("More than one account matches this login: 2 rows, ids [7731, 7981]"));

        MagicLinkController controller = new MagicLinkController(
                jwtTokenProvider,
                myUserDetailsService,
                activityLogger,
                mock(EmailService.class),
                courseRepository,
                userCourseRepository,
                oneTimeTokenService
        );

        UserUpsertException thrown = assertThrows(
                UserUpsertException.class,
                () -> controller.magicLogin(Map.of("token", "a-valid-token"))
        );

        assertEquals("More than one account matches this login: 2 rows, ids [7731, 7981]", thrown.getMessage());
        verifyNoInteractions(jwtTokenProvider, activityLogger, userCourseRepository);
    }
}
