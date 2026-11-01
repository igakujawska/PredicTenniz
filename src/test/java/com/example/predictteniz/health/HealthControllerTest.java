package com.example.predictteniz.health;

import com.example.predictteniz.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(SecurityConfiguration.class)
class HealthControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("PredictTeniz"));
    }

    @Test
    void publicAccessDoesNotExtendToOtherPaths() throws Exception {
        mvc.perform(get("/api/health/details").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/players").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publicAccessDoesNotExtendToOtherHttpMethods() throws Exception {
        mvc.perform(post("/api/health").with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
