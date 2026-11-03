package com.example.predictteniz.prediction.ml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class MlClient {
    private final RestClient mlRestClient;

    public List<MlContract.ModelInfo> models() {
        try {
            var result = mlRestClient.get().uri("/v1/models").retrieve().body(MlContract.ModelInfo[].class);
            if (result == null || result.length == 0 || Arrays.stream(result).anyMatch(model ->
                    model == null || model.version() == null || model.version().isBlank() || model.label() == null))
                throw badResponse();
            return List.copyOf(Arrays.asList(result));
        } catch (RestClientException error) {
            throw translate(error);
        }
    }

    public MlContract.BatchResult predict(MlContract.Batch batch) {
        try {
            var result = mlRestClient.post().uri("/v1/predict").body(batch).retrieve().body(MlContract.BatchResult.class);
            if (result == null || !batch.modelVersion().equals(result.modelVersion()) || !batch.asOf().equals(result.asOf())
                    || !batch.surface().equals(result.surface()) || batch.bestOf() != result.bestOf() || !batch.category().equals(result.category()) || result.predictions() == null)
                throw badResponse();
            return result;
        } catch (RestClientException error) {
            throw translate(error);
        }
    }

    private ResponseStatusException translate(RestClientException error) {
        log.debug("ML API request failed", error);
        if (error instanceof ResourceAccessException)
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Silnik ML jest niedostępny. Uruchom aplikację Python i spróbuj ponownie.", error);
        if (error instanceof RestClientResponseException response) {
            if (response.getStatusCode().value() == 422) {
                String detail = "Silnik ML odrzucił dane. Sprawdź datę i ustawienia predykcji.";
                try {
                    Map<?, ?> body = JsonMapper.builder().build().readValue(response.getResponseBodyAsString(), Map.class);
                    if (body.get("detail") instanceof String reason) {
                        detail = reason.contains("trained through")
                            ? "Model ML został wytrenowany na nowszych danych. Wybierz późniejszą datę analizy."
                            : "Silnik ML odrzucił dane: " + reason;
                    }
                } catch (Exception ignored) {
                    // A malformed error body must still preserve the upstream validation status.
                }
                return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, detail, error);
            }
            if (response.getStatusCode().is5xxServerError())
                return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Silnik ML zgłosił błąd. Spróbuj ponownie.", error);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nieprawidłowa odpowiedź silnika ML.", error);
    }

    private ResponseStatusException badResponse() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Odpowiedź silnika ML nie pasuje do kontraktu API.");
    }
}
