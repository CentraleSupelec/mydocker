package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A platform we do not operate must not be able to bind a launch to a privileged account, and the
 * refusal has to happen before anything is written: the upsert rewrites name and lastname on the
 * row it resolves, so a check placed after it would let a refused login deface an administrator.
 */
class MyUserDetailsServicePrivilegedOriginTest {
    private static final String ADMIN_EMAIL = "admin@example.org";

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private MyUserDetailsService service;
    private User admin;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        service = new MyUserDetailsService(userRepository, roleRepository);

        admin = new User();
        admin.setId(1L);
        admin.setEmail(ADMIN_EMAIL);
        admin.setUsername(ADMIN_EMAIL);
        admin.setName("Real");
        admin.setLastname("Administrator");
        admin.setEnabled(true);
        admin.setRoles(new ArrayList<>(List.of(role("ROLE_ADMIN"))));

        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(List.of(admin));
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    @Test
    void ltiLaunchCannotBindToAnAdministratorAccount() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upsertUserFromUntrustedOrigin(
                        null, ADMIN_EMAIL, "Attacker", "Attacker", TokenOrigin.LTI
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void refusedLaunchLeavesTheAdministratorProfileUntouched() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.upsertUserFromUntrustedOrigin(
                        null, ADMIN_EMAIL, "Attacker", "Attacker", TokenOrigin.LTI
                )
        );
        assertEquals("Real", admin.getName());
        assertEquals("Administrator", admin.getLastname());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void magicLinkCannotBindToAnAdministratorAccountEither() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findOrCreateMagicLinkUser(ADMIN_EMAIL)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void anOrdinaryAccountIsStillUpsertedFromLti() {
        User student = new User();
        student.setId(2L);
        student.setEmail("student@example.org");
        student.setUsername("student@example.org");
        student.setName("Old");
        student.setLastname("Name");
        student.setEnabled(true);
        student.setRoles(new ArrayList<>(List.of(role("ROLE_USER"))));
        when(userRepository.findByEmail("student@example.org")).thenReturn(List.of(student));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.upsertUserFromUntrustedOrigin(
                null, "student@example.org", "New", "Name", TokenOrigin.LTI
        );

        assertEquals("New", result.getName());
        verify(userRepository).save(student);
    }

    @Test
    void ourOwnProvidersMayStillBindToAnAdministratorAccount() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        User result = service.upsertUserFromUntrustedOrigin(
                null, ADMIN_EMAIL, "Real", "Administrator", TokenOrigin.CAS
        );

        assertEquals(admin.getId(), result.getId());
    }
}
