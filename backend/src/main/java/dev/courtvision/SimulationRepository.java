package dev.courtvision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class SimulationRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public SimulationRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public void save(DrawResult result) {
        try {
            jdbc.update("INSERT INTO simulation(id,title,surface,created_at,result_json) VALUES (?,?,?,?,?)",
                result.id(), result.title(), result.surface().name(), result.createdAt(), mapper.writeValueAsString(result));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize a result.", e); }
    }

    public List<Summary> recent() {
        return jdbc.query("SELECT id,title,surface,created_at FROM simulation ORDER BY created_at DESC LIMIT 20", (rs, row) ->
            new Summary(rs.getString("id"), rs.getString("title"), rs.getString("surface"), rs.getString("created_at")));
    }

    public DrawResult require(String id) {
        List<String> found = jdbc.queryForList("SELECT result_json FROM simulation WHERE id = ?", String.class, id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation not found.");
        try { return mapper.readValue(found.getFirst(), DrawResult.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not read a saved result.", e); }
    }
    public record Summary(String id, String title, String surface, String createdAt) {}
}
