package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.exception.UserUpsertException;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The resolver must still refuse an ambiguous login even though changeset 46 stops the database
 * from holding a colliding pair.
 *
 * <p>These two cases used to be covered by storing two rows that share an address. The unique
 * constraint makes that impossible, so the pair is stubbed instead. Deleting the cases was not an
 * option: a rolling deployment runs this code against a database where the changeset has not been
 * applied yet, and a restore from a pre-2026-09 dump brings the colliding rows back.
 */
@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceAmbiguityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private MyUserDetailsService myUserDetailsService;

    @Test
    void upsertUserRefusesAPairSharingAnAddress() {
        when(userRepository.findByEmail("first.last@example.com"))
                .thenReturn(List.of(user(1L, "short@example.com"), user(2L, "First.Last@example.com")));

        UserUpsertException thrown = assertThrows(
                UserUpsertException.class,
                () -> myUserDetailsService.upsertUser(
                        "short@example.com", "first.last@example.com", "First", "Last"
                )
        );

        // The message names the rows by id and carries no address: it reaches ERROR-level logs and
        // Sentry, and the ids are what an operator needs in order to merge the rows.
        assertTrue(thrown.getMessage().contains("ids [1, 2]"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("2 rows"), thrown.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upsertUserRefusesThePairEvenWhenOneRowIsDisabled() {
        // Measured on Virtual Desktop production on 2026-09-07, before the hand cleanup: 43 pairs
        // shared an address and 18 contained a row that the old deduplication path had disabled.
        // Those logins worked, because the old lookup filtered on enabled and therefore saw one
        // row. Resolution is state-agnostic now, so the pair is ambiguous and the login fails
        // loudly. If this test starts passing because the resolver filters on enabled again, that
        // is the June 2026 incident returning, not a fix.
        User enabled = user(11L, "short@example.com");
        User disabled = user(12L, "First.Last@example.com");
        disabled.setEnabled(false);
        when(userRepository.findByEmail("first.last@example.com")).thenReturn(List.of(enabled, disabled));

        assertThrows(
                UserUpsertException.class,
                () -> myUserDetailsService.upsertUser(null, "first.last@example.com", "First", "Last")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateMagicLinkUserRefusesAPairSharingAnAddress() {
        when(userRepository.findByEmail("first.last@example.com"))
                .thenReturn(List.of(user(21L, "short@example.com"), user(22L, "first.last@example.com")));

        assertThrows(
                UserUpsertException.class,
                () -> myUserDetailsService.findOrCreateMagicLinkUser("first.last@example.com")
        );
        verify(userRepository, never()).save(any());
    }

    private User user(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail("first.last@example.com");
        user.setName("First");
        user.setLastname("Last");
        return user;
    }
}
