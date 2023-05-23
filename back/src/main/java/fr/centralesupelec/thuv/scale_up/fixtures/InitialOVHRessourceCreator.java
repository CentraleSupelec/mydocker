package fr.centralesupelec.thuv.scale_up.fixtures;

import fr.centralesupelec.thuv.scale_up.model.OVHResource;
import fr.centralesupelec.thuv.scale_up.repository.OVHResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@SuppressWarnings("checkstyle:magicnumber")
public class InitialOVHRessourceCreator implements ApplicationListener<ContextRefreshedEvent> {
    boolean alreadySetup = false;

    private final OVHResourceRepository ovhResourceRepository;

    @Autowired
    public InitialOVHRessourceCreator(OVHResourceRepository ovhResourceRepository) {
        this.ovhResourceRepository = ovhResourceRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadySetup) {
            return;
        }
        createRessourceIfNotFound("b2-7", 7L, 2L);
        createRessourceIfNotFound("b2-15", 15L, 4L);
        createRessourceIfNotFound("b2-30", 30L, 8L);
        createRessourceIfNotFound("b2-60", 60L, 16L);
        createRessourceIfNotFound("b2-120", 120L, 32L);

        createRessourceIfNotFound("c2-7", 7L, 2L);
        createRessourceIfNotFound("c2-15", 15L, 4L);
        createRessourceIfNotFound("c2-30", 30L, 8L);
        createRessourceIfNotFound("c2-60", 60L, 16L);
        createRessourceIfNotFound("c2-120", 120L, 32L);

        createRessourceIfNotFound("r2-15", 15L, 2L);
        createRessourceIfNotFound("r2-30", 30L, 2L);
        createRessourceIfNotFound("r2-60", 60L, 4L);
        createRessourceIfNotFound("r2-120", 120L, 8L);
        createRessourceIfNotFound("r2-240", 240L, 16L);

        createRessourceIfNotFound("d2-2", 2L, 1L);
        createRessourceIfNotFound("d2-4", 4L, 2L);
        createRessourceIfNotFound("d2-8", 8L, 4L);

        alreadySetup = true;

    }

    @Transactional
    protected void createRessourceIfNotFound(String type, Long ramInGo, Long coreNumber) {
        Optional<OVHResource> ovhRessourceOptional = ovhResourceRepository.findByType(type);
        if (ovhRessourceOptional.isPresent()) {
            return;
        }

        ovhResourceRepository.save(
                new OVHResource()
                    .setType(type)
                    .setRamInGo(ramInGo)
                    .setCoreNumber(coreNumber)
        );
    }
}
