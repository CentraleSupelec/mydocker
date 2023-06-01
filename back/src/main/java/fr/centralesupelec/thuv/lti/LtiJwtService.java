package fr.centralesupelec.thuv.lti;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class LtiJwtService {
    private final LtiKeyProvider ltiKeyProvider;
    private final Algorithm ltiAlgorithm;
    @Value("${app.jwtExpirationInMs}") int expirationInMs;

    @Autowired
    public LtiJwtService(LtiKeyProvider ltiKeyProvider) {
        this.ltiKeyProvider = ltiKeyProvider;
        this.ltiAlgorithm = Algorithm.RSA256(this.ltiKeyProvider);
    }

    public String signPayload(Object payload) {
        Map<String, Object> headers = Map.ofEntries(
                Map.entry("typ", "JWT"),
                Map.entry("alg", this.ltiAlgorithm.getName()),
                Map.entry("kid", this.ltiKeyProvider.getPrivateKeyId())
        );
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};
        Map<String, Object> mapPayload = objectMapper.convertValue(payload, typeRef);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + this.expirationInMs);
        return JWT
                .create()
                .withHeader(headers)
                .withPayload(mapPayload)
                .withExpiresAt(expiryDate)
                .withIssuedAt(now)
                .sign(this.ltiAlgorithm)
                ;
    }

    public String getJwks() {
        return this.ltiKeyProvider.getJwks().toJson();
    }
}
