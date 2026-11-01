package com.example.predictteniz.player.importing;

import com.example.predictteniz.player.*;
import java.io.StringReader;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@Transactional
class RankingImportServiceTest {
    @Autowired RankingImportService importer;
    @Autowired PlayerRepository players;
    @Autowired PlayerExternalIdRepository identities;
    @Autowired PlayerRankingRepository rankings;

    @Test
    void importsLatestSnapshotForKnownPlayersAndSkipsRepeatedImport() throws Exception {
        var player = players.save(new Player("Ranking", "Test", "POL", null));
        identities.save(new PlayerExternalId(player, "SACKMANN_ATP", "42"));
        String csv = "ranking_date,rank,player,points\n20260601,8,42,100\n20260608,5,42,200\n20260608,1,999,500\n20260608,9,999,100\n";
        var first = importer.importLatest(new StringReader(csv));
        var repeated = importer.importLatest(new StringReader(csv));
        assertThat(first.created()).isEqualTo(1);
        assertThat(first.unknownPlayers()).isEqualTo(2);
        assertThat(repeated.created()).isZero();
        assertThat(repeated.alreadyKnown()).isEqualTo(1);
        assertThat(rankings.findAll()).singleElement().satisfies(ranking -> {
            assertThat(ranking.getPosition()).isEqualTo(5);
            assertThat(ranking.getRankingDate()).isEqualTo(LocalDate.of(2026, 6, 8));
        });
    }

    @Test
    void rejectsInvalidKnownRankingBeforeWritingAnyRows() {
        var player = players.save(new Player("Invalid", "Test", null, null));
        identities.save(new PlayerExternalId(player, "SACKMANN_ATP", "42"));
        String csv = "ranking_date,rank,player,points\n20260608,0,42,200\n";
        assertThatThrownBy(() -> importer.importLatest(new StringReader(csv)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(rankings.count()).isZero();
    }
    @Test
    void rejectsConflictingRankingsForKnownPlayer() {
        var player = players.save(new Player("Duplicate", "Test", null, null));
        identities.save(new PlayerExternalId(player, "SACKMANN_ATP", "42"));
        String csv = "ranking_date,rank,player,points\n20260608,2,42,200\n20260608,3,42,100\n";
        assertThatThrownBy(() -> importer.importLatest(new StringReader(csv)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate known");
        assertThat(rankings.count()).isZero();
    }
}
