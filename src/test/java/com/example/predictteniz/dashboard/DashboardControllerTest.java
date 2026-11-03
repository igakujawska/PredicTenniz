package com.example.predictteniz.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
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
    void backendRootRedirectsToTheIndependentFrontend() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost:4200"));
    }
}
