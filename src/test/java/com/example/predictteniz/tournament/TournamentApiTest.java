package com.example.predictteniz.tournament;

import com.example.predictteniz.player.*;
import com.example.predictteniz.prediction.ml.MlClient;
import com.example.predictteniz.prediction.ml.MlContract;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
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
    @MockitoBean MlClient ml;
    @Autowired MockMvc mvc;
    @Autowired PlayerRepository players;
    @Autowired PlayerRankingRepository rankings;
    @Autowired TournamentDrawRepository draws;

    @BeforeEach
    void configurePythonBoundary() {
        when(ml.models()).thenReturn(List.of(
            MlContract.ModelInfo.builder().version("ranking-baseline-v1").label("Ranking").trained(false).build(),
            MlContract.ModelInfo.builder().version("rank-logistic-v1").label("ML").trained(true)
                .trainedThrough(LocalDate.of(2024, 12, 31)).testYear(2025).testMatches(2483).build()));
        when(ml.predict(any())).thenAnswer(invocation -> {
            MlContract.Batch request = invocation.getArgument(0);
            if (request.modelVersion().equals("rank-logistic-v1") && request.asOf().isBefore(LocalDate.of(2024, 12, 31)))
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Model ML został wytrenowany na nowszych danych.");
            return new MlContract.BatchResult(request.modelVersion(), request.asOf(), request.surface(), request.bestOf(),
                request.pairs().stream().map(pair -> new MlContract.Outcome(pair.playerAId(), pair.playerBId(), 0.8, 0.2)).toList());
        });
    }

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
    @Test
    void savedMlDrawKeepsSurfaceFormatAndModelVersion() throws Exception {
        String request = body(rankedPair()).replace("\"playerIds\"", "\"surface\":\"CLAY\",\"bestOf\":5,\"modelVersion\":\"rank-logistic-v1\",\"playerIds\"");
        var response = mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.forecast.modelVersion").value("rank-logistic-v1"))
            .andExpect(jsonPath("$.forecast.surface").value("CLAY")).andExpect(jsonPath("$.forecast.bestOf").value(5))
            .andReturn().getResponse();
        mvc.perform(get(response.getHeader("Location"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.forecast.modelVersion").value("rank-logistic-v1"))
            .andExpect(jsonPath("$.forecast.surface").value("CLAY")).andExpect(jsonPath("$.forecast.bestOf").value(5));
    }

    @Test
    void modelTrainedInTheFutureCannotBeUsedInHistoricalForecast() throws Exception {
        var ids = rankedPair();
        for (long id : ids) rankings.save(new PlayerRanking(players.findById(id).orElseThrow(), RankingTour.ATP,
            LocalDate.of(2024, 6, 1), 8, 100, "TEST", Instant.now()));
        String request = body(ids).replace("2026-06-08", "2024-06-08")
            .replace("\"playerIds\"", "\"modelVersion\":\"rank-logistic-v1\",\"playerIds\"");
        mvc.perform(post("/api/tournaments/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("wytrenowany")));
        assertThat(draws.count()).isZero();
        mvc.perform(get("/api/tournaments/models")).andExpect(status().isOk())
            .andExpect(jsonPath("$[1].trained").value(true)).andExpect(jsonPath("$[1].testMatches").value(2483));
    }

    @Test
    void invalidMatchFormatDoesNotCreateADraw() throws Exception {
        String request = body(rankedPair()).replace("\"playerIds\"", "\"bestOf\":4,\"playerIds\"");
        mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isBadRequest());
        assertThat(draws.count()).isZero();
    }
    @Test
    void pythonOutageDoesNotCreateADraw() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Silnik ML jest niedostępny.")).when(ml).predict(any());
        mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(rankedPair())))
            .andExpect(status().isServiceUnavailable());
        assertThat(draws.count()).isZero();
    }

    @Test
    void allSixteenPlayersUseOneBatchOfOneHundredTwentyPairs() throws Exception {
        var ids = new java.util.ArrayList<Long>();
        for (int rank = 1; rank <= 16; rank++) {
            var player = players.save(new Player("Test", "Player " + rank, null, null));
            rankings.save(new PlayerRanking(player, RankingTour.ATP, LocalDate.of(2026, 6, 8), rank, 100, "TEST", Instant.now()));
            ids.add(player.getId());
        }
        mvc.perform(post("/api/tournaments/preview").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(ids)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.forecast.rounds.length()").value(4));
        verify(ml, times(1)).predict(argThat(request -> request.pairs().size() == 120));
    }
    @Test
    void updatingReordersPlayersAndRejectsStaleVersions() throws Exception {
        var ids = rankedPair();
        var created = mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(ids)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(0)).andReturn().getResponse();
        var location = created.getHeader("Location");
        String changed = body(List.of(ids.getLast(), ids.getFirst())).replace("API draw", "Nowa nazwa").replace("}", ",\"version\":0}");
        mvc.perform(put(location).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(changed))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.playerIds[0]").value(ids.getLast()));
        mvc.perform(put(location).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(changed))
            .andExpect(status().isConflict());
        mvc.perform(get(location)).andExpect(jsonPath("$.title").value("Nowa nazwa"));
        assertThat(draws.count()).isEqualTo(1);
    }

    @Test
    void savedDefinitionRemainsReadableWhenPythonIsDown() throws Exception {
        var ids = rankedPair();
        var created = mvc.perform(post("/api/tournaments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(ids)))
            .andReturn().getResponse();
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Silnik ML jest niedostępny.")).when(ml).predict(any());
        mvc.perform(get(created.getHeader("Location"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.playerIds[0]").value(ids.getFirst()))
            .andExpect(jsonPath("$.forecast").isEmpty()).andExpect(jsonPath("$.predictionError").isNotEmpty());
    }

}
