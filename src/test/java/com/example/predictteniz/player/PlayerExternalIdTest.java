package com.example.predictteniz.player;

import jakarta.persistence.EntityManager;
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
class PlayerExternalIdTest {
    @Autowired PlayerRepository players;
    @Autowired PlayerExternalIdRepository externalIds;
    @Autowired EntityManager entityManager;

    @Test
    void sourceIdentityFindsTheCorrectPlayerAfterReload() {
        Player player = players.save(new Player("Identity", "Probe", null, null));
        externalIds.saveAndFlush(new PlayerExternalId(player, "SACKMANN_ATP", "123"));
        Long playerId = player.getId();
        entityManager.clear();
        var mapping = externalIds.findBySourceAndExternalId("SACKMANN_ATP", "123").orElseThrow();
        assertThat(mapping.getPlayer().getId()).isEqualTo(playerId);
        assertThat(mapping.getPlayer().getLastName()).isEqualTo("Probe");
        assertThat(externalIds.findBySourceAndExternalId("SACKMANN_WTA", "123")).isEmpty();
    }
}
