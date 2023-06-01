package fr.centralesupelec.thuv.permissions.services;

import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseCreatorPermissionGenerator implements CreatorPermissionGenerator {
    private final CourseRepository courseRepository;

    @Override
    public List<String> generatePermissions(User user) {
        return courseRepository.findByCreator(user)
                .stream()
                .map(course -> "course." + course.getId() + ".creator")
                .collect(Collectors.toList());
    }
}
