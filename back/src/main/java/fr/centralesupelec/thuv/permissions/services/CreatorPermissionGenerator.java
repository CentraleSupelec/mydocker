package fr.centralesupelec.thuv.permissions.services;

import fr.centralesupelec.thuv.model.User;

import java.util.List;

public interface CreatorPermissionGenerator {
    List<String> generatePermissions(User user);
}
