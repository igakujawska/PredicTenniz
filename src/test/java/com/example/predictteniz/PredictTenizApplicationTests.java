package com.example.predictteniz;

import com.example.predictteniz.player.Player;
import jakarta.persistence.EntityManager;
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
class PredictTenizApplicationTests {

    @Autowired
    EntityManager entityManager;

    @Test
    void contextLoads() {
    }

    @Test
    void playerCanBeSavedAndLoadedFromPostgres() {
        Player player = new Player("Test", "Player", "POL", LocalDate.of(2000, 1, 15));
        entityManager.persist(player);
        entityManager.flush();
        Long id = player.getId();
        assertThat(id).isPositive();
        entityManager.clear();

        Player loaded = entityManager.find(Player.class, id);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getFirstName()).isEqualTo("Test");
        assertThat(loaded.getLastName()).isEqualTo("Player");
        assertThat(loaded.getCountryCode()).isEqualTo("POL");
        assertThat(loaded.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 15));
    }

    @Test
    void optionalPlayerDetailsCanBeMissing() {
        Player player = new Player("Optional", "Details", null, null);
        entityManager.persist(player);
        entityManager.flush();
        Long id = player.getId();
        entityManager.clear();

        Player loaded = entityManager.find(Player.class, id);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getCountryCode()).isNull();
        assertThat(loaded.getBirthDate()).isNull();
    }
}
