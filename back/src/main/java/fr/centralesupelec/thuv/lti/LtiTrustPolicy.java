package fr.centralesupelec.thuv.lti;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which registered platforms are trusted to assert a role, as opposed to merely being able to
 * launch. Any platform can be registered; being registered is not the same as being trusted to say
 * who is an instructor. Empty by default: no platform grants a role until it is listed.
 */
@Component
public class LtiTrustPolicy {
    private final Set<String> trustedIssuers;

    public LtiTrustPolicy(@Value("${lti.trusted-issuers:}") String trustedIssuers) {
        this.trustedIssuers = Arrays
                .stream(trustedIssuers.split(","))
                .map(String::trim)
                .filter(issuer -> !issuer.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean mayGrantTeacherRole(String issuer) {
        return null != issuer && this.trustedIssuers.contains(issuer);
    }
}
