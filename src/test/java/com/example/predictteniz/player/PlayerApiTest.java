package com.example.predictteniz.player;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@AutoConfigureMockMvc
@Transactional
class PlayerApiTest {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;

    @Test
    void emptyDatabaseReturnsAnEmptyPageWithoutLogin() throws Exception {
        mvc.perform(get("/api/players"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void returnsSortedPaginatedPlayerProfilesFromPostgres() throws Exception {
        Player alpha = new Player("Bea", "Alpha", "POL", LocalDate.of(2000, 1, 15));
        entityManager.persist(new Player("Zoe", "Zulu", null, null));
        entityManager.persist(alpha);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/players").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(alpha.getId()))
            .andExpect(jsonPath("$.items[0].firstName").value("Bea"))
            .andExpect(jsonPath("$.items[0].lastName").value("Alpha"))
            .andExpect(jsonPath("$.items[0].countryCode").value("POL"))
            .andExpect(jsonPath("$.items[0].birthDate").value("2000-01-15"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/players").param("size", "1").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].lastName").value("Zulu"));
    }

    @Test
    void rejectsInvalidPaginationWithProblemDetails() throws Exception {
        mvc.perform(get("/api/players").param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/players").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/players").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void publicReadAccessDoesNotEnableWrites() throws Exception {
        mvc.perform(post("/api/players").with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
