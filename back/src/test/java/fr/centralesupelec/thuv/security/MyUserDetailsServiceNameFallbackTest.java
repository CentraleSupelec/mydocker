package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An identity provider may send no first or last name (optional OIDC and LTI claims, a CAS response
 * without attributes). The account must still be created, with names derived from an address of
 * the form firstname.lastname@domain, else John / Doe. An existing account keeps its stored names
 * unless the provider sends new ones.
 */
class MyUserDetailsServiceNameFallbackTest {
    private static final String EXISTING_EMAIL = "alice.martin@example.test";
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private MyUserDetailsService service;
    private User alice;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        service = new MyUserDetailsService(userRepository, roleRepository);

        alice = new User();
        alice.setId(2L);
        alice.setEmail(EXISTING_EMAIL);
        alice.setUsername(EXISTING_EMAIL);
        alice.setName("Alice");
        alice.setLastname("Martin");
        alice.setEnabled(true);
        alice.setRoles(new ArrayList<>(List.of(role("ROLE_USER"))));

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role("ROLE_USER")));
        when(userRepository.findByEmail(EXISTING_EMAIL)).thenReturn(List.of(alice));
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private void assertNames(String name, String lastName) {
        User saved = savedUser();
        assertEquals(name, saved.getName());
        assertEquals(lastName, saved.getLastname());
    }

    @Test
    void missingNamesAreDerivedFromAFirstnameDotLastnameAddress() {
        service.upsertUser(null, "jean.dupont@example.test", null, null);
        assertNames("Jean", "Dupont");
    }

    @Test
    void blankNamesAreTreatedAsMissing() {
        service.upsertUser(null, "jean.dupont@example.test", "", "");
        assertNames("Jean", "Dupont");
    }

    @Test
    void anAddressWithoutDotGivesJohnDoe() {
        service.upsertUser(null, "jdupont@example.test", null, null);
        assertNames("John", "Doe");
    }

    @Test
    void anAddressWithSeveralDotsGivesJohnDoe() {
        service.upsertUser(null, "jean.paul.dupont@example.test", null, null);
        assertNames("John", "Doe");
    }

    @Test
    void aCasUserIdThatIsNotAnAddressGivesJohnDoe() {
        service.upsertUser(null, "cassuser", null, null);
        assertNames("John", "Doe");
    }

    @Test
    void onlyTheMissingNameIsFilled() {
        service.upsertUser(null, "jdupont@example.test", "", "Martin");
        assertNames("John", "Martin");
    }

    @Test
    void anExistingAccountKeepsItsNamesWhenNoneAreSent() {
        service.upsertUser(null, EXISTING_EMAIL, null, null);
        assertNames("Alice", "Martin");
    }

    @Test
    void anExistingAccountKeepsItsNamesWhenBlanksAreSent() {
        service.upsertUser(null, EXISTING_EMAIL, "", "");
        assertNames("Alice", "Martin");
    }

    @Test
    void anExistingAccountIsUpdatedWhenNamesAreSent() {
        service.upsertUser(null, EXISTING_EMAIL, "Bob", "Durand");
        assertNames("Bob", "Durand");
    }

    @Test
    void ltiLaunchWithoutNamesCreatesJohnDoe() {
        service.upsertUserFromUntrustedOrigin(null, "jdupont@example.test", null, null, TokenOrigin.LTI);
        assertNames("John", "Doe");
    }

    @Test
    void ltiLaunchWithBlankNamesKeepsTheStoredNames() {
        service.upsertUserFromUntrustedOrigin(null, EXISTING_EMAIL, "", "", TokenOrigin.LTI);
        assertNames("Alice", "Martin");
    }
}
