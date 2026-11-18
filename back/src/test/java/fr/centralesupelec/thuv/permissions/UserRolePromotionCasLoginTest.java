package fr.centralesupelec.thuv.permissions;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.dtos.UpdateUserDto;
import fr.centralesupelec.thuv.permissions.mappers.PermissionMapper;
import fr.centralesupelec.thuv.permissions.mappers.UserMapper;
import fr.centralesupelec.thuv.permissions.repository.PermissionRepository;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetailsService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The whole promotion, end to end without a database: the admin form sends back what CAS stored,
 * PUT /user/{id} must accept it and change only the role, and the user's next CAS login must find
 * the same row, so that the role applies.
 */
class UserRolePromotionCasLoginTest {
    private static final String CAS_PRINCIPAL = "casstudent";

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserController controller;
    private MyUserDetailsService loginService;
    private User row;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        when(roleRepository.getByName("ROLE_TEACHER")).thenReturn(new Role().setName("ROLE_TEACHER"));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role().setName("ROLE_USER")));

        row = new User()
                .setId(11L)
                .setUsername(CAS_PRINCIPAL)
                .setEmail(CAS_PRINCIPAL)
                .setName("Cas")
                .setLastname("Student");
        row.setRoles(new ArrayList<>(List.of(new Role().setName("ROLE_USER"))));

        // A repository that finds the one stored row by its current username or email, as the
        // database does, and answers save with the entity it is given.
        userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail(anyString())).thenAnswer(inv ->
                inv.getArgument(0).equals(row.getEmail()) ? List.of(row) : List.of());
        when(userRepository.findByUsername(anyString())).thenAnswer(inv ->
                inv.getArgument(0).equals(row.getUsername()) ? Optional.of(row) : Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        controller = new UserController(
                mock(RoleHierarchy.class),
                mock(PermissionRepository.class),
                userRepository,
                mock(PermissionMapper.class),
                new UserMapper(roleRepository),
                List.of()
        );
        loginService = new MyUserDetailsService(userRepository, roleRepository);
    }

    private static UpdateUserDto promotionPayload(String username, String email) {
        return new UpdateUserDto()
                .setUsername(username)
                .setEmail(email)
                .setName("Cas")
                .setLastname("Student")
                .setRoles(List.of("ROLE_TEACHER"));
    }

    private static Set<ConstraintViolation<UpdateUserDto>> validate(UpdateUserDto dto) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(dto);
        }
    }

    @Test
    void putWithTheStoredCasIdentityIsValidAndPromotesWithoutRenaming() {
        UpdateUserDto payload = promotionPayload(CAS_PRINCIPAL, CAS_PRINCIPAL);
        Set<ConstraintViolation<UpdateUserDto>> violations = validate(payload);
        assertTrue(violations.isEmpty(), "PUT payload rejected with: " + violations);

        controller.updateUser(row, payload);

        verify(userRepository).saveAndFlush(row);
        assertEquals(List.of("ROLE_TEACHER"), row.getRoles().stream().map(Role::getName).toList());
        assertEquals(CAS_PRINCIPAL, row.getUsername());
        assertEquals(CAS_PRINCIPAL, row.getEmail());
    }

    @Test
    void nextCasLoginAfterAPromotionFindsTheSameRow() {
        // A form that leaves the identity fields empty, as one that does not know them would.
        controller.updateUser(row, promotionPayload(null, ""));

        // CasController hands the bare principal to upsertUser as the email, username null.
        User loggedIn = loginService.upsertUser(null, CAS_PRINCIPAL, "Cas", "Student");

        assertSame(row, loggedIn, "the login created or returned a different account");
        assertTrue(loggedIn.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName())));
    }
}
