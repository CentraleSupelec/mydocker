package fr.centralesupelec.thuv.permissions.dtos;

import fr.centralesupelec.thuv.permissions.models.Permission;
import lombok.Data;

@Data
public class UpdatePermissionDto {
    private Permission.Type type;
    private Long userId;
    private Long id;
}
