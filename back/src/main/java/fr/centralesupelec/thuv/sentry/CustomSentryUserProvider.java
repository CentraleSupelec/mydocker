package fr.centralesupelec.thuv.sentry;

import io.sentry.protocol.User;
import io.sentry.spring.SentryUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CustomSentryUserProvider implements SentryUserProvider {
    @Override
    public User provideUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails) {
            String email = ((UserDetails) auth.getPrincipal()).getUsername();
            User user = new User();
            user.setEmail(email);
            return user;
        }
        return null;
    }
}
