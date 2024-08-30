package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;

@Primary
@Service
public class MyUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsService.class);
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
                .findByEmail(s)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User " + s + " not found !")
                );
        return new MyUserDetails(user.getRoles(), user.getEmail(), user.getId());
    }

    public User upsertUser(String email, String name, String lastName) {
        return this.upsertUser(new String[]{email}, name, lastName);
    }

    public User upsertUser(String[] emails, String name, String lastName) {
        List<User> users = userRepository.findByEmailIn(Arrays.stream(emails).toList());
        logger.debug(
                "Found users {} for emails {}",
                Arrays.toString(users.stream().map(User::getId).toArray()),
                Arrays.toString(emails)
        );
        User user;
        if (users.size() > 1) {
            logger.error("Multiple users found with emails {}, using the first one", Arrays.toString(emails));
            user = users.get(0);
        } else if (users.size() == 1) {
            user = users.get(0);
            logger.debug("Upserting user {}", user.getId());
        } else {
            user = new User();
            user.setEmail(emails[0]);
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
