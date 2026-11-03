package com.example.predictteniz.prediction.ml;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/** DTOs mirror contracts/ml-openapi.json; the Python service owns model execution. */
public final class MlContract {
    private MlContract() {}

    @Builder
    public record ModelInfo(String version, String label, boolean trained, LocalDate trainedThrough,
                            Integer testYear, Integer testMatches, Benchmarks metrics) {}
    public record Scores(double logLoss, double brierScore, double accuracy) {}
    public record Benchmarks(Scores ml, Scores baseline, Scores coinFlip) {}
    public record Pair(long playerAId, long playerBId, int rankA, int rankB, String sourceAId, String sourceBId) {}
    public record Batch(String modelVersion, LocalDate asOf, String surface, int bestOf, List<Pair> pairs) {}
    public record Outcome(long playerAId, long playerBId, double probabilityA, double probabilityB) {}
    public record BatchResult(String modelVersion, LocalDate asOf, String surface, int bestOf, List<Outcome> predictions) {}
}
