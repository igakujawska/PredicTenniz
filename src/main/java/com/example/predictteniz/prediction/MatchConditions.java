package com.example.predictteniz.prediction;

import java.util.Objects;

public record MatchConditions(Surface surface, int bestOf, String category) {
    public static final MatchConditions DEFAULT = new MatchConditions(Surface.HARD, 3);

    public MatchConditions(Surface surface, int bestOf) { this(surface, bestOf, "TOUR"); }

    public MatchConditions {
        if (category == null) category = "TOUR";
        if (!java.util.Set.of("TOUR", "ATP_250", "ATP_500", "MASTERS_1000", "GRAND_SLAM").contains(category)) throw new IllegalArgumentException("Unsupported tournament category.");
        Objects.requireNonNull(surface, "Surface is required.");
        if (bestOf != 3 && bestOf != 5) throw new IllegalArgumentException("Best-of must be 3 or 5.");
    }

    public enum Surface { HARD, CLAY, GRASS }
}
