package fr.centralesupelec.thuv.permissions.dtos;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Data
public class UpdateUserDto {
    @Email
    private String email;
    @NotEmpty
    private String name;
    @NotEmpty
    private String lastname;
    @Pattern(regexp = "ROLE_TEACHER|ROLE_ADMIN")
    private String role;
}
