package com.example.predictteniz.player.importing;

import com.example.predictteniz.player.PlayerExternalIdRepository;
import com.example.predictteniz.player.PlayerRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.docker.compose.skip.in-tests=false",
    "spring.flyway.schemas=player_entity_test",
    "spring.jpa.properties.hibernate.default_schema=player_entity_test"
})
@Transactional
class PlayerImportServiceTest {
    @Autowired PlayerImportService importer;
    @Autowired PlayerRepository players;
    @Autowired PlayerExternalIdRepository externalIds;

    @Test
    void repeatedImportDoesNotDuplicatePlayersAndNamesAreNotUsedAsIdentity() {
        var rows = List.of(row("101"), row("102"));
        assertThat(importer.importPlayers(rows, "SACKMANN_ATP").created()).isEqualTo(2);
        var repeated = importer.importPlayers(rows, " sackmann_atp ");
        assertThat(repeated.created()).isZero();
        assertThat(repeated.alreadyKnown()).isEqualTo(2);
        assertThat(players.count()).isEqualTo(2);
        assertThat(externalIds.count()).isEqualTo(2);
    }

    @Test
    void theSameExternalIdFromDifferentSourcesDoesNotMergePlayers() {
        importer.importPlayers(List.of(row("101")), "PROVIDER_A");
        importer.importPlayers(List.of(row("101")), "PROVIDER_B");
        assertThat(players.count()).isEqualTo(2);
        var first = externalIds.findBySourceAndExternalId("PROVIDER_A", "101").orElseThrow();
        var second = externalIds.findBySourceAndExternalId("PROVIDER_B", "101").orElseThrow();
        assertThat(first.getPlayer().getId()).isNotEqualTo(second.getPlayer().getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aFailedBatchRollsBackEarlierRows() {
        long playersBefore = players.count();
        long identitiesBefore = externalIds.count();
        var invalid = new PlayerCsvReader.PlayerRow("104", "Alex", "", "POL", null);
        assertThatThrownBy(() -> importer.importPlayers(List.of(row("103"), invalid), "ROLLBACK_TEST"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(players.count()).isEqualTo(playersBefore);
        assertThat(externalIds.count()).isEqualTo(identitiesBefore);
    }

    private PlayerCsvReader.PlayerRow row(String id) {
        return new PlayerCsvReader.PlayerRow(id, "Alex", "Probe", "POL", LocalDate.of(2000, 1, 15));
    }
}
