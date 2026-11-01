package com.example.predictteniz.prediction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PredictionModels {
    private final RankBaselineModel baseline;
    private final TrainedLogisticModel trained = new TrainedLogisticModel();

    public PredictionModels(RankBaselineModel baseline) { this.baseline = baseline; }

    public WinProbabilityModel resolve(String version, LocalDate asOf) {
        if (version == null || version.equals(baseline.version())) return baseline;
        if (!version.equals(trained.version()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieznana wersja modelu.");
        if (asOf.isBefore(trained.trainedThrough()))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Model ML jest wytrenowany na danych do " + trained.trainedThrough() + ". Wybierz późniejszą datę lub model bazowy.");
        return trained;
    }

    public List<Map<String, Object>> descriptions() {
        return List.of(Map.of("version", baseline.version(), "label", "Ranking · bazowy", "trained", false),
            trained.description());
    }
}
