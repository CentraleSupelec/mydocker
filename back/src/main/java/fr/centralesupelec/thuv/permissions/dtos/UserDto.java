package fr.centralesupelec.thuv.permissions.dtos;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String username;
    private String name;
    private String lastname;
    private String role;
    private Boolean enabled;
}
