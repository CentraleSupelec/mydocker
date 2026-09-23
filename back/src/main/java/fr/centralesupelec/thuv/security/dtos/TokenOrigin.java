package fr.centralesupelec.thuv.security.dtos;

public enum TokenOrigin {
    OIDC, CAS, LTI, MAGIC_LINK;

    /**
     * Administrator authority is available only from the identity providers we operate. A platform
     * we do not run must never be able to assert an administrator, whether it was registered by
     * mistake, taken over, or is hostile.
     */
    public boolean grantsPrivilegedAuthority() {
        return this == OIDC || this == CAS;
    }

    /**
     * Same rule, applied to the raw claim carried by a token. Fails closed: a missing or
     * unrecognised origin grants nothing.
     */
    public static boolean grantsPrivilegedAuthority(String origin) {
        if (null == origin) {
            return false;
        }
        try {
            return valueOf(origin).grantsPrivilegedAuthority();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
