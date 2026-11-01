package dev.courtvision;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TennisProvider {
    private final String key;
    private final RestClient client;

    public TennisProvider(@Value("${tennis.api-key:}") String key) {
        this.key = key;
        var http = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().baseUrl("https://api.api-tennis.com/tennis/").requestFactory(factory).build();
    }

    public boolean configured() { return !key.isBlank(); }

    public List<Fixture> fixtures(LocalDate date) {
        if (!configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Set TENNIS_API_KEY on the backend to connect API Tennis.");
        JsonNode response;
        try {
            response = client.get().uri(uri -> uri.queryParam("method", "get_fixtures")
                .queryParam("APIkey", key).queryParam("date_start", date).queryParam("date_stop", date)
                .queryParam("timezone", "Europe/Warsaw").build()).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            // Provider exceptions may contain the key in a URL. Never return or log their message.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tennis provider is unavailable or rejected the request.");
        }
        if (response == null || response.path("success").asInt() != 1)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tennis provider returned an unsuccessful response.");
        JsonNode rows = response.path("result");
        if (!rows.isArray()) return List.of();
        var result = new java.util.ArrayList<Fixture>();
        for (JsonNode row : rows) {
            if (result.size() == 100) break;
            result.add(new Fixture(row.path("event_key").asText(), row.path("event_date").asText(),
                row.path("event_time").asText(), row.path("event_first_player").asText(),
                row.path("event_second_player").asText(), row.path("tournament_name").asText(),
                row.path("event_status").asText()));
        }
        return result;
    }
    public record Fixture(String id, String date, String time, String first, String second, String tournament, String status) {}
}
