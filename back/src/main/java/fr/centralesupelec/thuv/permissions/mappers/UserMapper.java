package fr.centralesupelec.thuv.permissions.mappers;

import fr.centralesupelec.thuv.model.Role;
import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.dtos.UpdateUserDto;
import fr.centralesupelec.thuv.permissions.dtos.UserDto;
import fr.centralesupelec.thuv.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMapper {
    private final RoleRepository roleRepository;

    public UserDto convertToDto(User user) {
        return new UserDto()
                .setId(
                        user.getId()
                ).setName(
                        user.getName()
                )
                .setLastname(
                        user.getLastname()
                )
                .setEmail(
                        user.getEmail()
                )
                .setRole(
                        user.getRoles().stream().findAny().orElseGet(Role::new).getName()
                );
    }

    public void applyChange(User user, UpdateUserDto updateUserDto) {
        user
        .setEmail(
                updateUserDto.getEmail()
        )
        .setLastname(
                updateUserDto.getLastname()
        )
        .setName(
                updateUserDto.getName()
        );
        user.getRoles().clear();
        user.getRoles().add(
            roleRepository.getByName(
                    updateUserDto.getRole()
            )
        );
    }
}
