package fr.centralesupelec.thuv.lti;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LtiTrustPolicyTest {

    @Test
    void noIssuerIsTrustedByDefault() {
        LtiTrustPolicy policy = new LtiTrustPolicy("");
        assertFalse(policy.mayGrantTeacherRole("https://moodle.example.org"));
        assertFalse(policy.mayGrantTeacherRole(null));
    }

    @Test
    void onlyListedIssuersMayGrantTheTeacherRole() {
        LtiTrustPolicy policy = new LtiTrustPolicy("https://moodle.example.org, https://lms.example.net");
        assertTrue(policy.mayGrantTeacherRole("https://moodle.example.org"));
        assertTrue(policy.mayGrantTeacherRole("https://lms.example.net"));
        assertFalse(policy.mayGrantTeacherRole("https://attacker.example.com"));
    }

    @Test
    void matchingIsExact() {
        LtiTrustPolicy policy = new LtiTrustPolicy("https://moodle.example.org");
        assertFalse(policy.mayGrantTeacherRole("https://moodle.example.org.attacker.com"));
        assertFalse(policy.mayGrantTeacherRole("moodle.example.org"));
    }
}
