package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.MatchConditions;
import com.example.predictteniz.player.Player;
import com.example.predictteniz.player.PlayerRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@Transactional
class TournamentDrawPersistenceTest {
    @Autowired PlayerRepository players;
    @Autowired TournamentDrawRepository draws;
    @Autowired EntityManager entities;

    @Test
    void keepsTheExactDrawOrderAcrossADatabaseRoundTrip() {
        var first = players.save(new Player("First", "Test", null, null));
        var second = players.save(new Player("Second", "Test", null, null));
        var ids = List.of(second.getId(), first.getId());
        var saved = draws.saveAndFlush(new TournamentDraw("Test draw", LocalDate.of(2026, 6, 8), ids, new MatchConditions(MatchConditions.Surface.GRASS, 5), "rank-logistic-v1"));
        entities.clear();
        var loaded = draws.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getPlayerIds()).containsExactlyElementsOf(ids);
        assertThat(loaded.getTitle()).isEqualTo("Test draw");
        assertThat(loaded.getConditions()).isEqualTo(new MatchConditions(MatchConditions.Surface.GRASS, 5));
        assertThat(loaded.getModelVersion()).isEqualTo("rank-logistic-v1");
    }
}
