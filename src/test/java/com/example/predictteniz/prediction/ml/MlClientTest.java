package com.example.predictteniz.prediction.ml;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class MlClientTest {
    private HttpServer server;
    private MlClient client;
    private final AtomicReference<String> response = new AtomicReference<>();
    private final AtomicReference<String> request = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (var body = exchange.getResponseBody()) { body.write(bytes); }
        });
        server.start();
        client = new MlClient(RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
    }

    @AfterEach
    void stop() { server.stop(0); }

    private MlContract.Batch batch() {
        return new MlContract.Batch("ranking-baseline-v1", LocalDate.of(2026, 6, 8), "HARD", 3,
            List.of(new MlContract.Pair(1, 2, 1, 16, "100", "200")));
    }

    @Test
    void serializesTypedBatchAndReadsTypedPrediction() {
        response.set("""
            {"modelVersion":"ranking-baseline-v1","asOf":"2026-06-08","surface":"HARD","bestOf":3,
             "predictions":[{"playerAId":1,"playerBId":2,"probabilityA":0.8,"probabilityB":0.2}]}
            """);
        var result = client.predict(batch());
        assertThat(result.predictions().getFirst().probabilityA()).isEqualTo(0.8);
        assertThat(request.get()).contains("\"asOf\":\"2026-06-08\"", "\"sourceAId\":\"100\"");
    }

    @Test
    void exposesEngineOutageAndRejectsMalformedSuccessResponse() {
        response.set("not-json");
        assertThatThrownBy(() -> client.predict(batch())).isInstanceOfSatisfying(ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(502));
        response.set("[null]");
        assertThatThrownBy(client::models).isInstanceOfSatisfying(ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(502));
        status.set(503);
        response.set("{\"detail\":\"unavailable\"}");
        assertThatThrownBy(client::models).isInstanceOfSatisfying(ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }

    @Test
    void preservesValidationErrorsFromPython() {
        status.set(422);
        response.set("{\"detail\":\"Model was trained through 2024-12-31\"}");
        assertThatThrownBy(() -> client.predict(batch())).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode().value()).isEqualTo(422);
            assertThat(error.getReason()).contains("wytrenowany");
        });
    }
}
