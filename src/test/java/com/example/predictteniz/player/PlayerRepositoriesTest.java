package com.example.predictteniz.player;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@Transactional
class PlayerRepositoriesTest {

    @Autowired PlayerRepository players;
    @Autowired PlayerRankingRepository rankings;
    @Autowired EntityManager entityManager;

    @Test
    void latestRankingRespectsDatePlayerAndTour() {
        Long playerId = seedHistory();

        var ranking = rankings.findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
            playerId, RankingTour.ATP, LocalDate.of(2024, 1, 10)).orElseThrow();

        assertThat(ranking.getRankingDate()).isEqualTo(LocalDate.of(2024, 1, 8));
        assertThat(ranking.getPosition()).isEqualTo(6);
        assertThat(rankings.findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
            playerId, RankingTour.ATP, LocalDate.of(2024, 1, 1)).orElseThrow().getPosition()).isEqualTo(10);
    }

    @Test
    void bestRecordedRankingDoesNotUseFutureOrOtherTourResults() {
        Long playerId = seedHistory();

        assertThat(rankings.findBestRecordedPositionAsOf(playerId, RankingTour.ATP,
            LocalDate.of(2024, 1, 1))).contains(10);
        assertThat(rankings.findBestRecordedPositionAsOf(playerId, RankingTour.ATP,
            LocalDate.of(2024, 1, 20))).contains(6);
    }

    @Test
    void missingHistoryReturnsEmptyInsteadOfPositionZero() {
        Player player = players.saveAndFlush(new Player("No", "History", null, null));

        assertThat(rankings.findBestRecordedPositionAsOf(player.getId(), RankingTour.ATP,
            LocalDate.of(2024, 1, 1))).isEmpty();
        assertThat(rankings.findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
            player.getId(), RankingTour.ATP, LocalDate.of(2024, 1, 1))).isEmpty();
    }

    @Test
    void playerRepositorySupportsDeterministicPagination() {
        players.saveAllAndFlush(List.of(new Player("Zoe", "Zulu", null, null),
            new Player("Alex", "Alpha", null, null)));
        entityManager.clear();

        var firstPage = players.findAll(PageRequest.of(0, 1, Sort.by("lastName", "firstName", "id")));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Player::getLastName).containsExactly("Alpha");
        assertThat(players.findAll(PageRequest.of(1, 1, Sort.by("lastName", "firstName", "id")))
            .getContent()).extracting(Player::getLastName).containsExactly("Zulu");
    }

    private Long seedHistory() {
        Player player = players.save(new Player("Ranking", "Probe", null, null));
        Player other = players.save(new Player("Other", "Player", null, null));
        rankings.saveAllAndFlush(List.of(
            observation(player, RankingTour.ATP, 1, 10),
            observation(player, RankingTour.ATP, 8, 6),
            observation(player, RankingTour.ATP, 15, 9),
            observation(player, RankingTour.ATP, 22, 3),
            observation(player, RankingTour.WTA, 1, 1),
            observation(other, RankingTour.ATP, 1, 2)));
        Long id = player.getId();
        entityManager.clear();
        return id;
    }

    private PlayerRanking observation(Player player, RankingTour tour, int day, int position) {
        return new PlayerRanking(player, tour, LocalDate.of(2024, 1, day), position, null,
            "test", Instant.parse("2024-01-30T08:00:00Z"));
    }
}
