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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

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
        return new MyUserDetails(user.getRoles(), user.getUsername(), user.getId());
    }

    public User findUser(String username, String email) throws UserUpsertException {
        if (username == null) {
            logger.debug("username is null, searching only for email '{}'", email);
            List<User> usersWithCorrectEmail = userRepository.findByEnabledTrueAndEmail(email);
            logger.debug("Found {} users with email '{}'", usersWithCorrectEmail.size(), email);
            if (usersWithCorrectEmail.size() > 1) {
                throw new UserUpsertException(String.format("More than one user found for email '%s'", email));
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
            throw new UserUpsertException(String.format("More than one user found for email '%s'", email));
        }
        if (usersWithUsernameAsEmail.size() > 1) {
            throw new UserUpsertException(String.format("More than one user found for email '%s'", username));
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
            throw new UserUpsertException(String.format(
                    "Found %d users with matching username '%s' and with matching email '%s'",
                    usersInDb.size(), username, email
            ));
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
        User user = null;
        try {
            user = this.findUser(username, email);
        } catch (Exception e) {
            user = new User();
        }
        // Update with latest informations
        user.setName(name);
        user.setLastname(lastName);
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
