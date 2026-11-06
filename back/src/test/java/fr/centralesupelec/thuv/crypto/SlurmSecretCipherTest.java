package fr.centralesupelec.thuv.crypto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlurmSecretCipherTest {

    private static String key(byte fill) {
        byte[] raw = new byte[32];
        Arrays.fill(raw, fill);
        return Base64.getEncoder().encodeToString(raw);
    }

    @Test
    void roundTrip() {
        SlurmSecretCipher cipher = new SlurmSecretCipher(key((byte) 1), true);
        assertEquals("s3cret", cipher.decrypt(cipher.encrypt("s3cret")));
    }

    @Test
    void wrongKeyReportsKeyMismatch() {
        String stored = new SlurmSecretCipher(key((byte) 1), true).encrypt("s3cret");
        SlurmSecretCipher other = new SlurmSecretCipher(key((byte) 2), true);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> other.decrypt(stored));
        assertTrue(e.getMessage().contains("different slurm.secret-encryption-key"));
    }
}
