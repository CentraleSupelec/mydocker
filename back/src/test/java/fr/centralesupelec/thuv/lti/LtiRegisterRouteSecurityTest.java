package fr.centralesupelec.thuv.lti;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.auth0.jwk.JwkProvider;
import fr.centralesupelec.thuv.lti.dtos.OpenIdConfigurationDto;
import fr.centralesupelec.thuv.security.OIDCConfig;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The gate on the route the report was about. Registering an LTI platform is what let any account
 * introduce a signing key the launch path would then trust, so this is the authorization decision
 * the whole chain hung on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-test.properties",
        // Pin the key pair the context starts with, so this test does not depend on whichever
        // key files happen to be first on the test classpath.
        properties = {
                "lti.private-key-path=classpath:lti-test-private.pem",
                "lti.public-key-path=classpath:lti-test-public.pem"
        }
)
class LtiRegisterRouteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "ltiRestTemplate")
    private RestTemplate ltiRestTemplate;

    // Replaced rather than exercised: the real bean fetches the OIDC discovery document over the
    // network at startup, which would make this test depend on an external service being up.
    @MockitoBean(name = OIDCConfig.JWK_BEAN_NAME)
    private JwkProvider jwkProvider;

    @Test
    @WithMockUser(roles = "USER")
    void anOrdinaryAccountCannotRegisterAPlatform() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "https://attacker.example.com/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void aTeacherCannotRegisterAPlatformEither() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "https://attacker.example.com/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAnonymousCallerCannotRegisterAPlatform() throws Exception {
        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "https://attacker.example.com/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdministratorReachesTheHandler() throws Exception {
        // Unique per run: registration persists a row, and a repeated issuer would be answered
        // with a conflict by the handler rather than exercising the path under test.
        String issuer = "https://platform-" + UUID.randomUUID() + ".example.org";
        OpenIdConfigurationDto configuration = new OpenIdConfigurationDto();
        configuration.setIssuer(issuer);
        configuration.setJwksUri(issuer + "/jwks");
        configuration.setAuthorizationEndpoint(issuer + "/auth");
        configuration.setRegistrationEndpoint(issuer + "/register");
        when(ltiRestTemplate.getForObject(anyString(), eq(OpenIdConfigurationDto.class)))
                .thenReturn(configuration);
        when(ltiRestTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn("{\"client_id\": \"client-abc\"}");

        mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", issuer + "/.well-known/openid-configuration")
                        .param("registration_token", "token"))
                .andExpect(status().isOk());

        // Reaching the outbound call is what proves authorization let the request through.
        verify(ltiRestTemplate).getForObject(
                issuer + "/.well-known/openid-configuration", OpenIdConfigurationDto.class
        );
    }
}
