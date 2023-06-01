package fr.centralesupelec.thuv.lti;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import lombok.RequiredArgsConstructor;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@Service
@RequiredArgsConstructor
public class LtiKeyProvider implements RSAKeyProvider {
    private static final Logger logger = LoggerFactory.getLogger(LtiKeyProvider.class);
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final int checksumLength = 32;


    @Override
    public RSAPublicKey getPublicKeyById(String s) {
        if (s.equals(this.getPrivateKeyId())) {
            return this.publicKey;
        }
        return null;
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public String getPrivateKeyId() {
        Checksum crc32 = new CRC32();
        crc32.update(this.privateKey.getEncoded(), 0, this.checksumLength);
        return String.valueOf(crc32.getValue());
    }

    public JsonWebKeySet getJwks() {
        PublicJsonWebKey jwk;
        try {
            jwk = PublicJsonWebKey.Factory.newPublicJwk(this.publicKey);
            jwk.setKeyId(this.getPrivateKeyId());
            return new JsonWebKeySet(jwk);
        } catch (JoseException exception) {
            logger.error(String.format("Error generating jwks : %s", exception.getMessage()));
            return new JsonWebKeySet();
        }
    }
}
