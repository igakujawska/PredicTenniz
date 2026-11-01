package com.example.predictteniz.prediction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/** Inference only. Training and evaluation are reproducible in ml/train.py. */
public class TrainedLogisticModel implements WinProbabilityModel {
    private final Map<?, ?> artifact;
    private final Map<?, ?> coefficients;
    private final LocalDate trainedThrough;

    public TrainedLogisticModel() {
        try (var input = new ClassPathResource("models/rank-logistic-v1.json").getInputStream()) {
            artifact = JsonMapper.builder().build().readValue(input, Map.class);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load the trained tennis model.", error);
        }
        if (!Integer.valueOf(1).equals(artifact.get("schemaVersion")) || !version().equals(artifact.get("version")))
            throw new IllegalStateException("Unsupported tennis model artifact.");
        coefficients = (Map<?, ?>) artifact.get("coefficients");
        trainedThrough = LocalDate.parse((String) artifact.get("trainedThrough"));
        for (String key : new String[]{"hard", "clay", "grass", "bestOf5"}) {
            if (!(coefficients.get(key) instanceof Number value) || !Double.isFinite(value.doubleValue()))
                throw new IllegalStateException("Invalid model coefficient: " + key);
        }
    }

    @Override
    public String version() { return "rank-logistic-v1"; }

    @Override
    public double probability(int rankA, int rankB) {
        return probability(rankA, rankB, MatchConditions.DEFAULT);
    }

    @Override
    public double probability(int rankA, int rankB, MatchConditions conditions) {
        if (rankA < 1 || rankB < 1) throw new IllegalArgumentException("Ranks must be positive.");
        String surface = switch (conditions.surface()) {
            case HARD -> "hard";
            case CLAY -> "clay";
            case GRASS -> "grass";
        };
        double coefficient = ((Number) coefficients.get(surface)).doubleValue();
        if (conditions.bestOf() == 5) coefficient += ((Number) coefficients.get("bestOf5")).doubleValue();
        double score = coefficient * (Math.log(rankB) - Math.log(rankA));
        return 1 / (1 + Math.exp(-score));
    }

    public LocalDate trainedThrough() { return trainedThrough; }

    public Map<String, Object> description() {
        return Map.of("version", version(), "label", artifact.get("label"), "trained", true,
            "trainedThrough", trainedThrough.toString(), "testYear", artifact.get("testYear"),
            "testMatches", artifact.get("testMatches"), "metrics", artifact.get("metrics"));
    }
}
