package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.RoleRepository;
import fr.centralesupelec.thuv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

@Primary
@Service
public class MyUserDetailsService implements UserDetailsService {
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
        // Eager loading gets roles as well
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    return newUser;
                });
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
