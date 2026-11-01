package dev.courtvision;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:test;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DATABASE_USERNAME:sa}",
    "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
    "tennis.api-key="
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    private static final String DRAW = """
        {"title":"Test draw","surface":"HARD","playerIds":["sinner","alcaraz","fritz","hurkacz"],"prizes":[100,200,400],"currency":"EUR"}
        """;
    @Test void savesAndReloadsARealDatabaseResult() throws Exception {
        var response = mvc.perform(post("/api/simulations").contentType("application/json").content(DRAW))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.standings.length()").value(4)).andReturn();
        String id = mapper.readTree(response.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(get("/api/simulations/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Test draw"));
        mvc.perform(get("/api/simulations")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
    }
    @Test void validatesInputAndDoesNotPretendProviderIsConnected() throws Exception {
        mvc.perform(post("/api/simulations").contentType("application/json").content(DRAW.replace("\"fritz\"", "\"sinner\""))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/simulations").contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/simulations").contentType("application/json").content(DRAW.replace("[100,200,400]", "[100,null,400]"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/simulations").contentType("application/json").content(DRAW.replace("[100,200,400]", "[100,200,400.5]"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/simulations/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/api/integrations/fixtures?date=2026-11-01")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/players")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(16));
    }
    @Test void previewsDoNotCreateSavedRuns() throws Exception {
        String before = mvc.perform(get("/api/simulations")).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/draws/preview").contentType("application/json").content(DRAW))
            .andExpect(status().isOk()).andExpect(jsonPath("$.modelVersion").value("surface-elo-demo-v1"));
        mvc.perform(get("/api/simulations")).andExpect(content().json(before));
    }
}
