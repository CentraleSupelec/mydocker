package fr.centralesupelec.thuv.lti;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        // The repository's committed LTI keys are placeholders and cannot be parsed; the context
        // needs a real pair to start, and these are test-only.
        properties = {
                "lti.private-key-path=classpath:lti-test-private.pem",
                "lti.public-key-path=classpath:lti-test-public.pem"
        }
)
class LtiRegisterRouteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

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
    void anAdministratorReachesTheHandler() {
        // Authorization is what is under test. Past the gate the handler tries to fetch the
        // configuration document and fails on a closed port, which is itself the proof that the
        // request was let through: an unauthorized caller never gets that far.
        Exception thrown = assertThrows(
                Exception.class,
                () -> mockMvc.perform(get(LtiConfig.REGISTER_PATH)
                        .param("openid_configuration", "http://127.0.0.1:1/.well-known/openid-configuration")
                        .param("registration_token", "token"))
        );
        Throwable cause = thrown;
        while (null != cause.getCause() && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        assertInstanceOf(ConnectException.class, cause);
    }
}
