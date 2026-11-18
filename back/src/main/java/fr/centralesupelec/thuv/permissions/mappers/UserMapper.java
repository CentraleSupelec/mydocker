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
                .setUsername(
                        user.getUsername()
                )
                .setEnabled(
                        user.getEnabled()
                )
                .setRoles(
                        user.getRoles().stream()
                                .map(Role::getName)
                                .toList()
                );
    }

    public void applyChange(User user, UpdateUserDto updateUserDto) {
        // Username and email are the login identity: keep the stored value unless the payload
        // carries a different non-empty one, so the next login still finds this row
        if (isIdentityChange(user.getEmail(), updateUserDto.getEmail())) {
            user.setEmail(updateUserDto.getEmail());
        }
        if (isIdentityChange(user.getUsername(), updateUserDto.getUsername())) {
            user.setUsername(updateUserDto.getUsername());
        }
        user
        .setLastname(
                updateUserDto.getLastname()
        )
        .setName(
                updateUserDto.getName()
        );
        if (updateUserDto.getEnabled() != null) {
            user.setEnabled(updateUserDto.getEnabled());
        }
        user.getRoles().clear();
        user.getRoles().addAll(
                updateUserDto.getRoles().stream()
                        .map(roleRepository::getByName)
                        .toList()
        );
    }

    private static boolean isIdentityChange(String stored, String requested) {
        return requested != null && !requested.isEmpty() && !requested.equals(stored);
    }
}
