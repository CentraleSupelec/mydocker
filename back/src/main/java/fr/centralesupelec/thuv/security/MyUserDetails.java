package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.model.Role;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class MyUserDetails implements UserDetails {
    private final Collection<Role> roles;
    private final String email;
    private final Long userId;
    private final Boolean enabled;

    /**
     * The authority the session actually carries. The authentication filter rebuilds this principal
     * without privileged roles when the token was not issued by one of our own identity providers,
     * so this answers "may this session act as an administrator", which is not the same question as
     * "does this account hold ROLE_ADMIN in the database".
     */
    public boolean hasAdminAuthority() {
        return getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities();
    }

    private List<GrantedAuthority> getGrantedAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach((role) -> authorities.add(new SimpleGrantedAuthority(role.getName())));
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !Boolean.FALSE.equals(enabled);
    }
}
