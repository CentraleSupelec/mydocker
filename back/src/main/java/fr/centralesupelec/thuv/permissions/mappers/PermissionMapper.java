package fr.centralesupelec.thuv.permissions.mappers;

import fr.centralesupelec.thuv.permissions.exceptions.NoMapperException;
import fr.centralesupelec.thuv.permissions.models.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionMapper {
    private final List<SubTypePermissionMapper> permissionMapperList;

    public String convertPermission(Permission permission) {
        return permissionMapperList.stream()
                .flatMap(m -> m.convertPermission(permission))
                .findAny()
                .orElseThrow(NoMapperException::new);
    }
}
