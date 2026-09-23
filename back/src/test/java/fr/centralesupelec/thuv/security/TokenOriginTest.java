package fr.centralesupelec.thuv.security;

import fr.centralesupelec.thuv.security.dtos.TokenOrigin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenOriginTest {

    @Test
    void ownIdentityProvidersGrantPrivilegedAuthority() {
        assertTrue(TokenOrigin.CAS.grantsPrivilegedAuthority());
        assertTrue(TokenOrigin.OIDC.grantsPrivilegedAuthority());
    }

    @Test
    void federatedOriginsDoNot() {
        assertFalse(TokenOrigin.LTI.grantsPrivilegedAuthority());
        assertFalse(TokenOrigin.MAGIC_LINK.grantsPrivilegedAuthority());
    }

    @Test
    void unknownAndMissingOriginsFailClosed() {
        assertFalse(TokenOrigin.grantsPrivilegedAuthority(null));
        assertFalse(TokenOrigin.grantsPrivilegedAuthority(""));
        assertFalse(TokenOrigin.grantsPrivilegedAuthority("SOMETHING_ELSE"));
        assertFalse(TokenOrigin.grantsPrivilegedAuthority("cas"));
    }

    @Test
    void claimValuesWrittenByTheProviderStillMatch() {
        assertTrue(TokenOrigin.grantsPrivilegedAuthority(TokenOrigin.CAS.toString()));
        assertFalse(TokenOrigin.grantsPrivilegedAuthority(TokenOrigin.LTI.toString()));
    }
}
