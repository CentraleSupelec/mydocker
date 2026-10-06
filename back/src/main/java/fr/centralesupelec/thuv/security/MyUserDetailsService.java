package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.exception.PrivilegedAccountRefusedException;
import fr.centralesupelec.thuv.exception.UserUpsertException;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Primary
@Service
public class MyUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(MyUserDetailsService.class);
    private static final String PRIVILEGED_ROLE = "ROLE_ADMIN";
    private static final String PRIVILEGED_ORIGIN_MESSAGE =
            "This login method cannot open a session for a privileged account";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public MyUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsername(s)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User " + s + " not found !")
                );
        return new MyUserDetails(user.getRoles(), user.getUsername(), user.getId(), user.getEnabled());
    }

    /**
     * Names the colliding accounts by id rather than by address. This message reaches ERROR-level
     * logs and Sentry, so it must not carry an email address; the ids are also what an operator
     * needs in order to merge the rows.
     */
    private static String formatAmbiguousAccountsMessage(Collection<User> users) {
        String ids = users.stream()
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.joining(", "));
        return String.format("More than one account matches this login: %d rows, ids [%s]", users.size(), ids);
    }

    /**
     * Single email-primary resolver for every auth path. State-agnostic: a banned
     * (enabled=false) row is found and rejected via {@link #assertUserEnabled}, never hidden.
     * Returns empty when no account matches; the caller decides whether to create one.
     */
    private Optional<User> resolveExisting(String username, String email) {
        List<User> byEmail = email == null ? List.of() : userRepository.findByEmail(email);
        if (byEmail.size() > 1) {
            throw new UserUpsertException(formatAmbiguousAccountsMessage(byEmail));
        }
        return byEmail.stream()
                .findFirst()
                .or(() -> username == null ? Optional.empty() : userRepository.findByUsername(username))
                .map(user -> {
                    assertUserEnabled(user);
                    return user;
                });
    }

    public User upsertUser(String username, String email, String name, String lastName) {
        // CAS/LTI pass username == null (email is their identity). Use email as the username
        // for fallback LOOKUP and NEW-user creation only.
        String lookupUsername = username != null ? username : email;
        return upsert(resolveExisting(lookupUsername, email), username, email, name, lastName);
    }

    /**
     * Login path for a provider we do not operate. A platform that is compromised, hostile, or
     * simply registered by mistake can claim any address it likes, so it must not be able to claim
     * one that belongs to a privileged account.
     *
     * <p>The refusal happens on the resolved account, before any write. {@link #fillUserInformation}
     * updates name and lastname on the row it is given, so a check placed after the upsert would
     * let a refused login still rewrite an administrator's profile. One resolution, one entity,
     * no second lookup.
     */
    public User upsertUserFromUntrustedOrigin(
            String username, String email, String name, String lastName, TokenOrigin origin
    ) {
        String lookupUsername = username != null ? username : email;
        Optional<User> existing = resolveExisting(lookupUsername, email);
        assertOriginMayBind(existing, origin);
        return upsert(existing, username, email, name, lastName);
    }

    private User upsert(Optional<User> existing, String username, String email, String name, String lastName) {
        User user = existing.orElseGet(User::new);
        // Never let a null-username provider overwrite an existing row's username (breaks JWT
        // subject continuity / can collide). Keep the row's username; seed email only when new.
        String usernameToSet = username != null
                ? username
                : (user.getUsername() != null ? user.getUsername() : email);
        return fillUserInformation(user, usernameToSet, email, name, lastName);
    }

    private void assertOriginMayBind(Optional<User> existing, TokenOrigin origin) {
        // No exemption by origin: a method named "from untrusted origin" that lets some origins
        // through is a bypass waiting for its first caller. Providers we operate use upsertUser.
        if (existing.isEmpty()) {
            return;
        }
        if (isPrivileged(existing.get())) {
            logger.warn("Refused {} login binding to privileged account id {}", origin, existing.get().getId());
            throw new PrivilegedAccountRefusedException(PRIVILEGED_ORIGIN_MESSAGE);
        }
    }

    private static boolean isPrivileged(User user) {
        return user.getRoles().stream().anyMatch(role -> PRIVILEGED_ROLE.equals(role.getName()));
    }

    public User findOrCreateMagicLinkUser(String email) {
        // Reuse an unambiguous existing account by email/username; create a guest only if none.
        Optional<User> existing = resolveExisting(email, email);
        assertOriginMayBind(existing, TokenOrigin.MAGIC_LINK);
        return existing
                .orElseGet(() -> fillUserInformation(new User(), email, email, "Invité", "Invité"));
    }

    public void assertUserEnabled(User user) {
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new DisabledException("Account disabled");
        }
    }

    public User fillUserInformation(User user, String username, String email, String name, String lastName) {
        Optional.ofNullable(name).ifPresent(user::setName);
        Optional.ofNullable(lastName).ifPresent(user::setLastname);
        Optional.ofNullable(username).ifPresent(user::setUsername);
        Optional.ofNullable(email).ifPresent(user::setEmail);

        if (user.getRoles().isEmpty()) {
            List<Role> roles = new ArrayList<>();
            // Role should always exist as it is initiated at app start
            roles.add(roleRepository.findByName("ROLE_USER").get());
            user.setRoles(roles);
        }
        userRepository.save(user);
        return user;
    }

    public void ensureTeacher(User user) {
        Collection<Role> userRoles = user.getRoles();
        if (userRoles.stream().noneMatch(role -> role.getName().equals("ROLE_TEACHER"))) {
            userRoles.add(roleRepository.findByName("ROLE_TEACHER").get());
            user.setRoles(userRoles);
            userRepository.save(user);
        }
    }
}
