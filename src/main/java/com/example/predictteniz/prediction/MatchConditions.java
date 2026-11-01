package com.example.predictteniz.prediction;

import java.util.Objects;

public record MatchConditions(Surface surface, int bestOf) {
    public static final MatchConditions DEFAULT = new MatchConditions(Surface.HARD, 3);

    public MatchConditions {
        Objects.requireNonNull(surface, "Surface is required.");
        if (bestOf != 3 && bestOf != 5) throw new IllegalArgumentException("Best-of must be 3 or 5.");
    }

    public enum Surface { HARD, CLAY, GRASS }
}
