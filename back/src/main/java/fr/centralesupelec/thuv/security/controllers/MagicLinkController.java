package fr.centralesupelec.thuv.security.controllers;

import fr.centralesupelec.thuv.activity_logging.model.LogAction;
import fr.centralesupelec.thuv.activity_logging.services.ActivityLogger;
import fr.centralesupelec.thuv.mail.EmailService;
import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.model.UserCourse;
import fr.centralesupelec.thuv.repository.CourseRepository;
import fr.centralesupelec.thuv.repository.UserCourseRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth/magic-link")
public class MagicLinkController {
    private static final Logger logger = LoggerFactory.getLogger(MagicLinkController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final ActivityLogger activityLogger;
    private final EmailService emailService;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final UserRepository userRepository;
    private final MyUserDetailsService myUserDetailsService;

    @Value("${app.magic_link.expiration_in_minutes}")
    private long magicLinkExpirationInMinutes;

    @Autowired
    public MagicLinkController(
            JwtTokenProvider jwtTokenProvider,
            MyUserDetailsService myUserDetailsService,
            ActivityLogger activityLogger,
            EmailService emailService,
            CourseRepository courseRepository,
            UserCourseRepository userCourseRepository,
            OneTimeTokenService oneTimeTokenService,
            UserRepository userRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.activityLogger = activityLogger;
        this.emailService = emailService;
        this.courseRepository = courseRepository;
        this.userCourseRepository = userCourseRepository;
        this.oneTimeTokenService = oneTimeTokenService;
        this.userRepository = userRepository;
        this.myUserDetailsService = myUserDetailsService;
    }

    @PostMapping
    public ResponseEntity<?> requestMagicLink(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String courseUuid = payload.get("courseUuid");

        if (email == null || courseUuid == null) {
            return ResponseEntity.badRequest().body("Email and courseUuid are required.");
        }

        Optional<Course> course = courseRepository.findByUuid(UUID.fromString(courseUuid));

        if (course.isPresent()) {
            if (!course.get().isExternalAccess() || course.get().getExternalAccessExpirationDate().isBefore(LocalDateTime.now())) {
                logger.error("External access for course (uuid=%s) denied.", courseUuid);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(String.format("External access for course (uuid=%s) denied", courseUuid));
            }
        } else {
            logger.error("Course with uuid %s not found", courseUuid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("Course uuid %s not found", courseUuid));
        }

        try {
            GenerateOneTimeTokenRequest ottRequest = new GenerateOneTimeTokenRequest(
                email + '/' + courseUuid,
                Duration.ofMinutes(magicLinkExpirationInMinutes)
            );
            OneTimeToken ott = oneTimeTokenService.generate(ottRequest);

            emailService.sendMagicLinkEmail(email, course.get(), ott.getTokenValue());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Sentry.captureException(e);
            logger.error("Failed to generate or send magic link", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send magic link");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> magicLogin(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().body("Token is required.");
        }

        try {
            OneTimeTokenAuthenticationToken ottAuthToken = new OneTimeTokenAuthenticationToken(token);
            OneTimeToken ott = oneTimeTokenService.consume(ottAuthToken);
            String[] parts = ott.getUsername().split("/");
            String email = parts[0];
            String courseUuid = parts[1];

            User user = userRepository.findByUsername(email)
                .orElseGet(() -> this.myUserDetailsService.fillUserInformation(new User(), email, email, "Invité", "Invité")
            );
            
            Optional<Course> course = courseRepository.findByUuid(UUID.fromString(courseUuid));
            
            if (course.isPresent()) {
                if (!course.get().isExternalAccess() || course.get().getExternalAccessExpirationDate().isBefore(LocalDateTime.now())) {
                    logger.error(String.format("External access for course (uuid=%s) denied", courseUuid));
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        String.format("External access for course (uuid=%s) denied", courseUuid)
                    );
                }
            } else {
                logger.error(String.format("Course with uuid %s not found", courseUuid));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format("Course uuid %s not found", courseUuid));
            }

            userCourseRepository.findByUserIdAndCourseId(user.getId(), course.get().getId()).ifPresentOrElse(
                (UserCourse userCourse) -> { },
                () -> {
                    UserCourse userCourse = (new UserCourse()).setCourse(course.get()).setUser(user);
                    userCourse = userCourseRepository.saveAndFlush(userCourse);
                }
            );

            String sessionJwt = jwtTokenProvider.generateToken(
                    user.getUsername(),
                    user.getEmail(),
                    TokenOrigin.MAGIC_LINK
            );

            activityLogger.log(LogAction.USER_LOGIN_MAGIC_LINK, user);

            return ResponseEntity.ok(Map.of(
                    "token", sessionJwt,
                    "courseId", course.get().getId()
            ));
        } catch (Exception ex) {
            Sentry.captureException(ex);
            logger.error("Invalid magic token", ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired magic link");
        }
    }
}
