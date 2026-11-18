package fr.centralesupelec.thuv.permissions.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Collection;

@Data
public class UpdateUserDto {
    // No @Email: CAS stores the bare principal (e.g. "casteacher") in both fields
    private String email;
    private String username;
    @NotEmpty
    private String name;
    @NotEmpty
    private String lastname;
    private Collection<@Pattern(regexp = "ROLE_USER|ROLE_TEACHER|ROLE_ADMIN") String> roles;
    private Boolean enabled;
}
