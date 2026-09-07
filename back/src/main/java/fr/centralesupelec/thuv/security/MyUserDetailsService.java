package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.exception.UserUpsertException;
import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
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
    private static String ambiguousAccounts(Collection<User> users) {
        String ids = users.stream()
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.joining(", "));
        return String.format("More than one account matches this login: %d rows, ids [%s]", users.size(), ids);
    }

    public User findUser(String username, String email) throws UserUpsertException {
        if (username == null) {
            logger.debug("username is null, searching only for email '{}'", email);
            List<User> usersWithCorrectEmail = userRepository.findByEnabledTrueAndEmail(email);
            logger.debug("Found {} users with email '{}'", usersWithCorrectEmail.size(), email);
            if (usersWithCorrectEmail.size() > 1) {
                throw new UserUpsertException(ambiguousAccounts(usersWithCorrectEmail));
            }
            Optional<User> userWithEmailAsUsername = userRepository.findByEnabledTrueAndUsername(email);
            logger.debug("Found user with username '{}': {}", email, userWithEmailAsUsername.orElse(null));
            if (
                    userWithEmailAsUsername.isPresent()
                            && usersWithCorrectEmail.size() == 1
                            && !userWithEmailAsUsername.get().equals(usersWithCorrectEmail.get(0))
            ) {
                usersWithCorrectEmail.forEach(user -> {
                    user.setEnabled(false);
                    if (StringUtils.isBlank(user.getName())) {
                        user.setName("placeholder");
                    }
                    if (StringUtils.isBlank(user.getLastname())) {
                        user.setLastname("placeholder");
                    }
                    userRepository.save(user);
                });
                userWithEmailAsUsername.get().setEmail(email);
                return userWithEmailAsUsername.get();
            }
            if (usersWithCorrectEmail.size() == 1) {
                return usersWithCorrectEmail.get(0);
            }
            User user = userWithEmailAsUsername.orElse((new User()).setUsername(email));
            user.setEmail(email);
            return user;
        }
        Optional<User> userWithCorrectUsername = userRepository.findByEnabledTrueAndUsername(username);
        logger.debug("User with correct username '{}' : {} ", username, userWithCorrectUsername.orElse(null));
        Optional<User> userWithEmailAsUsername = userRepository.findByEnabledTrueAndUsername(email);
        logger.debug("User with email as username '{}' : {} ", email, userWithEmailAsUsername.orElse(null));
        List<User> usersWithUsernameAsEmail = userRepository.findByEnabledTrueAndEmail(username);
        logger.debug("Found {} users with username '{}' as email", usersWithUsernameAsEmail.size(), email);
        List<User> usersWithCorrectEmail = userRepository.findByEnabledTrueAndEmail(email);
        logger.debug("Found {} users with email '{}'", usersWithCorrectEmail.size(), email);
        if (usersWithCorrectEmail.size() > 1) {
            throw new UserUpsertException(ambiguousAccounts(usersWithCorrectEmail));
        }
        if (usersWithUsernameAsEmail.size() > 1) {
            throw new UserUpsertException(ambiguousAccounts(usersWithUsernameAsEmail));
        }
        if (userWithCorrectUsername.isPresent()) {
            userWithEmailAsUsername.ifPresent(user -> {
                if (user.equals(userWithCorrectUsername.get())) {
                    return;
                }
                logger.debug("Disabling user with email as username {} ", user);
                user.setEnabled(false);
                if (StringUtils.isBlank(user.getEmail())) {
                    user.setEmail(email);
                }
                if (StringUtils.isBlank(user.getName())) {
                    user.setName("placeholder");
                }
                if (StringUtils.isBlank(user.getLastname())) {
                    user.setLastname("placeholder");
                }
                userRepository.save(user);
            });
            usersWithCorrectEmail.forEach(user -> {
                if (user.equals(userWithCorrectUsername.get())) {
                    return;
                }
                logger.debug("Disabling user with correct email {} ", user);
                user.setEnabled(false);
                if (StringUtils.isBlank(user.getEmail())) {
                    user.setEmail(email);
                }
                if (StringUtils.isBlank(user.getName())) {
                    user.setName("placeholder");
                }
                if (StringUtils.isBlank(user.getLastname())) {
                    user.setLastname("placeholder");
                }
                userRepository.save(user);
            });
            usersWithUsernameAsEmail.forEach(user -> {
                if (user.equals(userWithCorrectUsername.get())) {
                    return;
                }
                logger.debug("Disabling user with username as email {} ", user);
                user.setEnabled(false);
                if (StringUtils.isBlank(user.getEmail())) {
                    user.setEmail(email);
                }
                if (StringUtils.isBlank(user.getName())) {
                    user.setName("placeholder");
                }
                if (StringUtils.isBlank(user.getLastname())) {
                    user.setLastname("placeholder");
                }
                userRepository.save(user);
            });
            User user = userWithCorrectUsername.get();
            user.setEmail(email);
            logger.debug("Found user with username '{}': {}", username, user);
            return user;
        }
        HashSet<User> usersInDb = new HashSet<>();
        if (userWithEmailAsUsername.isPresent()) {
            usersInDb.add(userWithEmailAsUsername.get());
        }
        if (usersWithCorrectEmail.size() == 1) {
            usersInDb.add(usersWithCorrectEmail.get(0));
        }
        if (usersWithUsernameAsEmail.size() == 1) {
            usersInDb.add(usersWithUsernameAsEmail.get(0));
        }

        if (usersInDb.size() > 1) {
            throw new UserUpsertException(ambiguousAccounts(usersInDb));
        }
        if (usersWithCorrectEmail.size() == 1) {
            User user = usersWithCorrectEmail.get(0);
            logger.debug("Updating username for user {} to {} ", user, username);
            user.setUsername(username);
            return user;
        }
        if (usersWithUsernameAsEmail.size() == 1) {
            User user = usersWithUsernameAsEmail.get(0);
            logger.debug("Updating username for user {} to {} ", user, username);
            user.setUsername(username);
            return user;
        }
        User user = userWithEmailAsUsername.orElse(new User());
        user.setEmail(email).setUsername(username);
        return user;
    }

    public User upsertUser(String username, String email, String name, String lastName) {
        User user = this.findUser(username, email);
        assertUserEnabled(user);
        return fillUserInformation(user, username, email, name, lastName);
    }

    public User findOrCreateMagicLinkUser(String email) {
        // Resolve existing accounts before creating a guest; disabled accounts must be rejected, not hidden.
        List<User> usersWithCorrectEmail = userRepository.findByEmail(email);
        if (usersWithCorrectEmail.size() > 1) {
            throw new UserUpsertException(ambiguousAccounts(usersWithCorrectEmail));
        }

        User user = usersWithCorrectEmail
                .stream()
                .findFirst()
                .or(() -> userRepository.findByUsername(email))
                .orElse(null);

        if (user == null) {
            return fillUserInformation(new User(), email, email, "Invité", "Invité");
        }
        assertUserEnabled(user);
        return user;
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
