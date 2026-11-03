package com.example.predictteniz.prediction.ml;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/** DTOs mirror contracts/ml-openapi.json; the Python service owns model execution. */
public final class MlContract {
    private MlContract() {}

    @Builder
    public record ModelInfo(String version, String label, boolean trained, LocalDate trainedThrough,
                            Integer testYear, Integer testMatches, Benchmarks metrics,
                            LocalDate dataThrough, Integer trainMatches, Integer historyMatches, List<String> featureNames,
                            List<FeatureGain> featureImportance, Map<String, CategoryScore> categoryScores, List<String> notes) {}
    public record Scores(double logLoss, double brierScore, double accuracy) {}
    public record Benchmarks(Scores ml, Scores baseline, Scores coinFlip, Scores logistic, Scores elo) {}
    public record FeatureGain(String feature, double gainShare) {}
    public record CategoryScore(int matches, Scores model, Scores ranking) {}
    public record Pair(long playerAId, long playerBId, int rankA, int rankB, String sourceAId, String sourceBId) {}
    public record Batch(String modelVersion, LocalDate asOf, String surface, int bestOf, List<Pair> pairs, String category) {
        public Batch { if (category == null) category = "TOUR"; }
        public Batch(String version, LocalDate date, String surface, int bestOf, List<Pair> pairs) { this(version,date,surface,bestOf,pairs,"TOUR"); }
    }
    public record Outcome(long playerAId, long playerBId, double probabilityA, double probabilityB) {}
    public record BatchResult(String modelVersion, LocalDate asOf, String surface, int bestOf, List<Outcome> predictions, String category) {
        public BatchResult { if (category == null) category = "TOUR"; }
        public BatchResult(String version, LocalDate date, String surface, int bestOf, List<Outcome> predictions) { this(version,date,surface,bestOf,predictions,"TOUR"); }
    }
}
