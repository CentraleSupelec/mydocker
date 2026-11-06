package fr.centralesupelec.thuv.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

// Transparently encrypts SlurmCluster.secretKey at rest.
//
// Registered as a Spring bean so Hibernate resolves it through Spring's bean
// container and injects the cipher. It previously reached the cipher through a
// static field set in the cipher's constructor, and passed the value through
// unchanged whenever that field was still null - which meant a converter built
// before the Spring context was ready wrote the secret to the database in clear
// There is no pass-through path any more: if the cipher is
// missing, persistence fails rather than silently storing plaintext.
@Component
@Converter
public class SlurmSecretKeyConverter implements AttributeConverter<String, String> {

    private final SlurmSecretCipher cipher;

    public SlurmSecretKeyConverter(SlurmSecretCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return requireCipher().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return requireCipher().decrypt(dbData);
    }

    private SlurmSecretCipher requireCipher() {
        if (cipher == null) {
            throw new IllegalStateException(
                    "SlurmSecretKeyConverter was built without a SlurmSecretCipher; "
                            + "refusing to read or write a Slurm cluster secret");
        }
        return cipher;
    }
}
