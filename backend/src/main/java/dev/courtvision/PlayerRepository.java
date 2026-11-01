package dev.courtvision;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerRepository {
    private final JdbcTemplate jdbc;
    public PlayerRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Player> findAll() {
        return jdbc.query("SELECT * FROM player ORDER BY hard_elo DESC, id", (rs, row) ->
            new Player(rs.getString("id"), rs.getString("name"), rs.getString("country"),
                rs.getDouble("hard_elo"), rs.getDouble("clay_elo"), rs.getDouble("grass_elo")));
    }

    public Player require(String id) {
        return findAll().stream().filter(p -> p.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + id));
    }
}
