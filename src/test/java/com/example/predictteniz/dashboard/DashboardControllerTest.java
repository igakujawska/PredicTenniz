package com.example.predictteniz.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@AutoConfigureMockMvc
class DashboardControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void dashboardIsPublicAndIncludesCsrfTokenForSimulationRequests() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk()).andExpect(view().name("dashboard"))
            .andExpect(content().string(containsString("name=\"_csrf\" content=\"")))
            .andExpect(content().string(containsString("PredictTeniz")));
        mvc.perform(get("/assets/dashboard.css")).andExpect(status().isOk());
    }
}
