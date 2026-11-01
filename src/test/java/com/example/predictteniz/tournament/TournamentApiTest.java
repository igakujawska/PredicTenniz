package com.example.predictteniz.tournament;

import com.example.predictteniz.player.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@AutoConfigureMockMvc
@Transactional
class TournamentApiTest {
    @Autowired MockMvc mvc;
    @Autowired PlayerRepository players;
    @Autowired PlayerRankingRepository rankings;
    @Autowired TournamentDrawRepository draws;

    private List<Long> rankedPair() {
        var a = players.save(new Player("A", "Test", "POL", null));
        var b = players.save(new Player("B", "Test", "ESP", null));
        rankings.save(new PlayerRanking(a, RankingTour.ATP, LocalDate.of(2026, 6, 8), 1, 100, "TEST", Instant.now()));
        rankings.save(new PlayerRanking(b, RankingTour.ATP, LocalDate.of(2026, 6, 8), 16, 10, "TEST", Instant.now()));
        return List.of(a.getId(), b.getId());
    }

    private String body(List<Long> ids) {
        return "{\"title\":\"API draw\",\"asOf\":\"2026-06-08\",\"playerIds\":" + ids + "}";
    }

    @Test
    void previewDoesNotSaveAndCreateCanBeLoadedAgain() throws Exception {
        var ids = rankedPair();
        mvc.perform(post("/api/tournaments/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(ids)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.forecast.titleChances[0].probability").value(0.8));
        assertThat(draws.count()).isZero();
        var response = mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(ids)))
            .andExpect(status().isCreated()).andReturn().getResponse();
        mvc.perform(get(response.getHeader("Location")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("API draw"))
            .andExpect(jsonPath("$.forecast.rounds[0].matches[0].playerA.id").value(ids.getFirst()));
        assertThat(draws.count()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicatePlayersAndMissingRankingsWithoutSaving() throws Exception {
        var ids = rankedPair();
        mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(body(List.of(ids.getFirst(), ids.getFirst())))).andExpect(status().isBadRequest());
        var unranked = players.save(new Player("Unranked", "Test", null, null));
        mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(body(List.of(ids.getFirst(), unranked.getId())))).andExpect(status().isUnprocessableEntity());
        assertThat(draws.count()).isZero();
    }

    @Test
    void csrfRemainsRequiredForPublicSimulationRequests() throws Exception {
        mvc.perform(post("/api/tournaments/preview").contentType(MediaType.APPLICATION_JSON)
            .content(body(List.of(1L, 2L)))).andExpect(status().isForbidden());
    }

    @Test
    void catalogExposesAvailableRankingDateAndOrderedPlayers() throws Exception {
        rankedPair();
        mvc.perform(get("/api/tournaments/players").param("asOf", "2026-06-08"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.latestRankingDate").value("2026-06-08"))
            .andExpect(jsonPath("$.players[0].position").value(1));
    }
}
