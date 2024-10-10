package fr.centralesupelec.thuv.permissions;

import fr.centralesupelec.thuv.model.User;
import fr.centralesupelec.thuv.permissions.dtos.UpdateUserDto;
import fr.centralesupelec.thuv.permissions.dtos.UserDto;
import fr.centralesupelec.thuv.permissions.exceptions.UserAlreadyExistException;
import fr.centralesupelec.thuv.permissions.mappers.PermissionMapper;
import fr.centralesupelec.thuv.permissions.mappers.UserMapper;
import fr.centralesupelec.thuv.permissions.repository.PermissionRepository;
import fr.centralesupelec.thuv.permissions.services.CreatorPermissionGenerator;
import fr.centralesupelec.thuv.repository.UserRepository;
import fr.centralesupelec.thuv.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final RoleHierarchy roleHierarchy;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final List<CreatorPermissionGenerator> creatorPermissionGenerators;

    @GetMapping("roles/")
    public List<String> getRoles(
        @AuthenticationPrincipal(errorOnInvalidType = true) final MyUserDetails userDetails
    ) {
        User user = userRepository.getReferenceById(userDetails.getUserId());
        List<String> authorities = roleHierarchy
                .getReachableGrantedAuthorities(
                        userDetails.getAuthorities()
                )
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        authorities.addAll(
                permissionRepository.findByUser(user)
                    .stream()
                    .map(permissionMapper::convertPermission)
                    .collect(Collectors.toList())
        );
        creatorPermissionGenerators.forEach(
                g -> authorities.addAll(g.generatePermissions(user))
        );
        return authorities;
    }

    @GetMapping("search")
    @PreAuthorize("hasRole('TEACHER')")
    public List<UserDto> searchUser(
            @RequestParam(value = "search") @NotNull String search
    ) {
        return userRepository.findDistinctByEmailContainingAndRolesNameIsNot(
                search, "ROLE_USER"
        )
                .stream()
                .map(userMapper::convertToDto)
                .collect(Collectors.toList());
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public void createUser(
            @RequestBody @Valid UpdateUserDto updateUserDto
    ) {
        if (userRepository.existsByUsername(updateUserDto.getUsername())) {
            throw new UserAlreadyExistException();
        }
        User user = new User();
        userMapper.applyChange(user, updateUserDto);
        userRepository.save(user);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUser(
            @PathVariable("id") User user,
            @RequestBody @Valid UpdateUserDto updateUserDto
    ) {
        userMapper.applyChange(user, updateUserDto);
        userRepository.saveAndFlush(user);
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> getUsers(
            @RequestParam(value = "search") @NotNull String search,
            @RequestParam(value = "roles") @NotNull List<String> roles,
            final Pageable pageable
    ) {
        if (StringUtils.isEmpty(search)) {
            return userRepository.findDistinctByRolesNameIn(
                            roles, pageable
                    )
                    .map(userMapper::convertToDto);
        }

        return userRepository.findDistinctByEmailContainingAndRolesNameIn(
                        search, roles, pageable
                )
                .map(userMapper::convertToDto);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto getUser(
            @PathVariable("id") @NotNull User user
    ) {
        return userMapper.convertToDto(user);
    }
}
