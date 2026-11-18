package fr.centralesupelec.thuv.permissions.mappers;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.dtos.UpdateUserDto;
import fr.centralesupelec.thuv.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Username and email are the login identity: CAS and the other providers look the row up by them.
 * An admin update that blanks them makes the next login miss the row and create a second account,
 * so the granted role stops applying. An update writes username or email only when the payload
 * value is non-empty and differs from the stored one; otherwise the stored value stays. Creating a
 * user (nothing stored yet) still sets them.
 */
class UserMapperIdentityTest {
    private static final String CAS_PRINCIPAL = "casteacher";

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.getByName("ROLE_USER")).thenReturn(role("ROLE_USER"));
        when(roleRepository.getByName("ROLE_TEACHER")).thenReturn(role("ROLE_TEACHER"));
        mapper = new UserMapper(roleRepository);
    }

    private static Role role(String name) {
        return new Role().setName(name);
    }

    private static User casUser() {
        User user = new User()
                .setId(7L)
                .setUsername(CAS_PRINCIPAL)
                .setEmail(CAS_PRINCIPAL)
                .setName("Cas")
                .setLastname("Teacher");
        user.setRoles(new ArrayList<>(List.of(role("ROLE_USER"))));
        return user;
    }

    private static UpdateUserDto promotion(String username, String email) {
        return new UpdateUserDto()
                .setUsername(username)
                .setEmail(email)
                .setName("Cas")
                .setLastname("Teacher")
                .setRoles(List.of("ROLE_TEACHER"));
    }

    @Test
    void roleChangeKeepsIdentityWhenPayloadOmitsIt() {
        User user = casUser();

        mapper.applyChange(user, promotion(null, null));

        assertEquals(List.of("ROLE_TEACHER"), user.getRoles().stream().map(Role::getName).toList());
        assertEquals(CAS_PRINCIPAL, user.getUsername());
        assertEquals(CAS_PRINCIPAL, user.getEmail());
    }

    @Test
    void roleChangeKeepsIdentityWhenPayloadValuesAreEmpty() {
        User user = casUser();

        mapper.applyChange(user, promotion("", ""));

        assertEquals(List.of("ROLE_TEACHER"), user.getRoles().stream().map(Role::getName).toList());
        assertEquals(CAS_PRINCIPAL, user.getUsername());
        assertEquals(CAS_PRINCIPAL, user.getEmail());
    }

    @Test
    void updateWritesAChangedNonEmptyIdentity() {
        User user = casUser();

        mapper.applyChange(user, promotion("renamed.teacher", "renamed.teacher@example.org"));

        assertEquals("renamed.teacher", user.getUsername());
        assertEquals("renamed.teacher@example.org", user.getEmail());
        assertEquals(List.of("ROLE_TEACHER"), user.getRoles().stream().map(Role::getName).toList());
    }

    @Test
    void creatingAUserStillSetsIdentityFromThePayload() {
        User user = new User();

        mapper.applyChange(user, promotion("new.user@example.org", "new.user@example.org"));

        assertEquals("new.user@example.org", user.getUsername());
        assertEquals("new.user@example.org", user.getEmail());
        assertTrue(user.getRoles().stream().anyMatch(r -> "ROLE_TEACHER".equals(r.getName())));
    }
}
