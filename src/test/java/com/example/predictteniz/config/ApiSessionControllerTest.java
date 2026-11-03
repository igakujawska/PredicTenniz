package com.example.predictteniz.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@AutoConfigureMockMvc
class ApiSessionControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void separateFrontendCanObtainAndUseACsrfToken() throws Exception {
        var response = mvc.perform(get("/api/session")).andExpect(status().isOk()).andReturn();
        Map<?, ?> token = JsonMapper.builder().build().readValue(response.getResponse().getContentAsString(), Map.class);
        assertThat(token.get("token")).isInstanceOf(String.class);
        var session = (MockHttpSession) response.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(post("/api/tournaments/preview").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/tournaments/preview").session(session).header((String) token.get("headerName"), token.get("token"))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }
}
