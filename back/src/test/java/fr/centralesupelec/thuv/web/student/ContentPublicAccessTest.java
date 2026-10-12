package fr.centralesupelec.thuv.web.student;

import fr.centralesupelec.thuv.SecurityConfig;
import fr.centralesupelec.thuv.model.Content;
import fr.centralesupelec.thuv.repository.ContentRepository;
import fr.centralesupelec.thuv.security.JwtAuthenticationEntryPoint;
import fr.centralesupelec.thuv.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Only the login-announcement block is readable without being logged in, so the login page can
 * show it; every other block keeps requiring a logged-in user.
 */
@WebMvcTest(controllers = ContentController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class ContentPublicAccessTest {
    @Autowired MockMvc mvc;
    @MockitoBean ContentRepository contentRepository;
    @MockitoBean(name = "userDetailsService") UserDetailsService userDetailsService;
    @MockitoBean JdbcTemplate jdbcTemplate;
    @MockitoBean JwtTokenProvider jwtTokenProvider;

    private void stub(String slug, boolean enabled) {
        Content content = new Content();
        content.setId(1L);
        content.setTitle(slug);
        content.setSlug(slug);
        content.setRichText("<p>" + slug + " text</p>");
        content.setEnabled(enabled);
        when(contentRepository.findBySlug(slug)).thenReturn(Optional.of(content));
    }

    @Test
    void anonymousReadsEnabledLoginAnnouncement() throws Exception {
        stub("login-announcement", true);
        mvc.perform(get("/content/login-announcement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.richText").value("<p>login-announcement text</p>"));
    }

    @Test
    void anonymousGetsNotFoundForDisabledLoginAnnouncement() throws Exception {
        stub("login-announcement", false);
        mvc.perform(get("/content/login-announcement"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousGetsNotFoundForAbsentLoginAnnouncement() throws Exception {
        when(contentRepository.findBySlug("login-announcement")).thenReturn(Optional.empty());
        mvc.perform(get("/content/login-announcement"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousIsRefusedWelcome() throws Exception {
        stub("welcome", true);
        mvc.perform(get("/content/welcome"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousIsRefusedOtherSlug() throws Exception {
        stub("some-other-block", true);
        mvc.perform(get("/content/some-other-block"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userReadsWelcome() throws Exception {
        stub("welcome", true);
        mvc.perform(get("/content/welcome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.richText").value("<p>welcome text</p>"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userReadsLoginAnnouncement() throws Exception {
        stub("login-announcement", true);
        mvc.perform(get("/content/login-announcement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.richText").value("<p>login-announcement text</p>"));
    }
}
