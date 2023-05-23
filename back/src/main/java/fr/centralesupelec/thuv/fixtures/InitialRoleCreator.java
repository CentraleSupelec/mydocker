package fr.centralesupelec.thuv.fixtures;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class InitialRoleCreator implements ApplicationListener<ContextRefreshedEvent> {

    boolean alreadySetup = false;

    private final RoleRepository roleRepository;

    @Autowired
    public InitialRoleCreator(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadySetup) {
            return;
        }
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_TEACHER");
        createRoleIfNotFound("ROLE_USER");
        alreadySetup = true;

    }

    @Transactional
    protected void createRoleIfNotFound(String name) {
        Optional<Role> optionalRole = roleRepository.findByName(name);
        if (optionalRole.isPresent()) {
            return;
        }

        Role role = new Role()
                .setName(name);
        roleRepository.save(role);
    }
}
