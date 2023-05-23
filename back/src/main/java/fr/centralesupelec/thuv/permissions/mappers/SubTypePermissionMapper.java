package fr.centralesupelec.thuv.permissions.mappers;

import fr.centralesupelec.thuv.permissions.models.Permission;

import java.util.stream.Stream;

public interface SubTypePermissionMapper {
    Stream<String> convertPermission(Permission permission);
}
