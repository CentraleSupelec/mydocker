package fr.centralesupelec.thuv.permissions.dtos;

import lombok.Data;

import java.util.Collection;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String username;
    private String name;
    private String lastname;
    private Collection<String> roles;
    private Boolean enabled;
}
