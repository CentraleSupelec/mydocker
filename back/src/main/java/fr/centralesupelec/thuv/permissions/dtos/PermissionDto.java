package fr.centralesupelec.thuv.permissions.dtos;

import fr.centralesupelec.thuv.permissions.models.Permission;
import lombok.Data;

@Data
public class PermissionDto {
    private Long id;
    private Permission.Type type;
    private UserDto user;
}
