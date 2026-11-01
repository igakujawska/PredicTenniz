package com.example.predictteniz.player;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
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
class PlayerRankingPersistenceTest {

    @Autowired
    EntityManager entityManager;

    @Test
    void rankingRoundTripPreservesPlayerTourDateAndProvenance() {
        Player player = new Player("Ranking", "Probe", "POL", null);
        entityManager.persist(player);
        Instant retrievedAt = Instant.parse("2024-01-08T08:00:00Z");
        PlayerRanking ranking = new PlayerRanking(player, RankingTour.ATP,
            LocalDate.of(2024, 1, 8), 6, 1200, "test", retrievedAt);
        entityManager.persist(ranking);
        entityManager.flush();
        Long rankingId = ranking.getId();
        Long playerId = player.getId();
        entityManager.clear();

        PlayerRanking loaded = entityManager.find(PlayerRanking.class, rankingId);

        assertThat(loaded.getPlayer().getId()).isEqualTo(playerId);
        assertThat(loaded.getPlayer().getLastName()).isEqualTo("Probe");
        assertThat(loaded.getTour()).isEqualTo(RankingTour.ATP);
        assertThat(loaded.getRankingDate()).isEqualTo(LocalDate.of(2024, 1, 8));
        assertThat(loaded.getPosition()).isEqualTo(6);
        assertThat(loaded.getPoints()).isEqualTo(1200);
        assertThat(loaded.getSource()).isEqualTo("test");
        assertThat(loaded.getRetrievedAt()).isEqualTo(retrievedAt);
    }

    @Test
    void rankingPointsCanBeUnknown() {
        Player player = new Player("Optional", "Points", null, null);
        entityManager.persist(player);
        PlayerRanking ranking = new PlayerRanking(player, RankingTour.WTA,
            LocalDate.of(2024, 1, 8), 12, null, "test", Instant.parse("2024-01-08T08:00:00Z"));
        entityManager.persist(ranking);
        entityManager.flush();
        Long id = ranking.getId();
        entityManager.clear();

        assertThat(entityManager.find(PlayerRanking.class, id).getPoints()).isNull();
    }
}
