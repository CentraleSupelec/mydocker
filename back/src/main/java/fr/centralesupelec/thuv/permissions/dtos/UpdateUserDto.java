package fr.centralesupelec.thuv.permissions.dtos;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Collection;

@Data
public class UpdateUserDto {
    @Email
    private String email;
    @Email
    private String username;
    @NotEmpty
    private String name;
    @NotEmpty
    private String lastname;
    private Collection<@Pattern(regexp = "ROLE_USER|ROLE_TEACHER|ROLE_ADMIN") String> roles;
    private Boolean enabled;
}
