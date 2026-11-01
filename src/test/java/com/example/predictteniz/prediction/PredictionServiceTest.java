package com.example.predictteniz.prediction;

import com.example.predictteniz.player.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@Transactional
class PredictionServiceTest {
    @Autowired PredictionService service;
    @Autowired PlayerRepository players;
    @Autowired PlayerRankingRepository rankings;

    @Test
    void historicalPredictionNeverReadsAFutureRanking() {
        var player = players.save(new Player("Historical", "Test", null, null));
        rankings.save(new PlayerRanking(player, RankingTour.ATP, LocalDate.of(2026, 6, 1), 8, 200, "TEST", Instant.now()));
        rankings.save(new PlayerRanking(player, RankingTour.ATP, LocalDate.of(2026, 7, 1), 1, 800, "TEST", Instant.now()));
        var snapshot = service.snapshots(List.of(player.getId()), LocalDate.of(2026, 6, 15)).getFirst();
        assertThat(snapshot.position()).isEqualTo(8);
        assertThat(snapshot.rankingDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void missingRankingProducesAnExplicitErrorInsteadOfInventingStrength() {
        var player = players.save(new Player("Unranked", "Test", null, null));
        assertThatThrownBy(() -> service.snapshots(List.of(player.getId()), LocalDate.of(2026, 6, 1)))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("422");
        assertThat(service.catalog(LocalDate.of(2026, 6, 1)).players()).isEmpty();
    }
}
