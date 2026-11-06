package fr.centralesupelec.thuv.crypto;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// AES-256-GCM encryption for secrets at rest. The key is a base64
// 32-byte value delivered from Ansible vault via the SLURM_SECRET_ENCRYPTION_KEY
// environment variable.
//
// The cipher fails closed on the operation that matters:
//   - a configured key that is not exactly 32 bytes aborts startup rather than
//     silently degrading;
//   - with no key, WRITING a secret is refused outright rather than stored in
//     clear, while reads of legacy plaintext still work so existing rows stay
//     usable;
//   - slurm.secret-encryption-required additionally turns a missing key into a
//     startup failure. It defaults to FALSE so that shipping this code cannot
//     stop a back-end that is already running without a key; provisioning turns
//     it on once the key is actually delivered.
//
// Stored form is "slurm-gcm-v1:" + base64(iv || ciphertext+tag). Values without
// the marker are legacy plaintext and are returned unchanged on read, so
// introducing the key does not break rows written before it existed.
@Component
public class SlurmSecretCipher {

    private static final Logger logger = LoggerFactory.getLogger(SlurmSecretCipher.class);
    // Versioned and specific enough that it is not plausibly the start of an
    // operator-chosen secret. The previous "gcm:" marker was short enough to
    // collide, and a colliding plaintext was stored unencrypted.
    private static final String PREFIX = "slurm-gcm-v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SlurmSecretCipher(
            @Value("${slurm.secret-encryption-key:}") String keyBase64,
            @Value("${slurm.secret-encryption-required:false}") boolean required
    ) {
        if (StringUtils.isBlank(keyBase64)) {
            if (required) {
                throw new IllegalStateException(
                        "slurm.secret-encryption-key is not set. Set it (32 random bytes, base64) "
                                + "or set slurm.secret-encryption-required=false for a local or test run "
                                + "where Slurm cluster secrets do not need to be protected at rest.");
            }
            this.key = null;
            logger.warn("slurm.secret-encryption-key is not set and encryption is not required; "
                    + "storing a Slurm cluster secret will be refused.");
            return;
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(keyBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("slurm.secret-encryption-key is not valid base64", e);
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "slurm.secret-encryption-key must decode to exactly " + KEY_BYTES
                            + " bytes for AES-256, got " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public boolean isEnabled() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        // Never fall back to writing the value in clear: without a key the only
        // safe outcome is to refuse the write.
        if (key == null) {
            throw new IllegalStateException(
                    "Cannot store a Slurm cluster secret: slurm.secret-encryption-key is not set");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt secret", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        if (key == null) {
            throw new IllegalStateException(
                    "Encrypted secret present but slurm.secret-encryption-key is not set");
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] plaintext = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new IllegalStateException(
                    "Failed to decrypt Slurm cluster secret: it was encrypted with a different "
                            + "slurm.secret-encryption-key than the one configured. Restore the original "
                            + "key or re-enter the cluster secret.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt secret", e);
        }
    }
}
