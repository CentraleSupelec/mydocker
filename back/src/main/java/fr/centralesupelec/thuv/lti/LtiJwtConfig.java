package fr.centralesupelec.thuv.lti;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.FileReader;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@RequiredArgsConstructor
public class LtiJwtConfig {
    private static final Logger logger = LoggerFactory.getLogger(LtiJwtConfig.class);
    @Value("${lti.private-key-path}")
    private Resource privateKeyPath;
    @Value("${lti.public-key-path}")
    private Resource publicKeyPath;


    @Bean
    public RSAPublicKey publicKey() throws Exception {
        try (
                FileReader keyReader = new FileReader(this.publicKeyPath.getFile());
                PEMParser pemParser = new PEMParser(keyReader)
        ) {
            SubjectPublicKeyInfo pemObject = (SubjectPublicKeyInfo) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return (RSAPublicKey) converter.getPublicKey(pemObject);
        }
    }

    @Bean
    public RSAPrivateKey privateKey() throws Exception {
        try (
                FileReader keyReader = new FileReader(this.privateKeyPath.getFile());
                PEMParser pemParser = new PEMParser(keyReader)
        ) {
            PEMKeyPair pemObject = (PEMKeyPair) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return (RSAPrivateKey) converter.getPrivateKey(pemObject.getPrivateKeyInfo());
        }
    }
}
