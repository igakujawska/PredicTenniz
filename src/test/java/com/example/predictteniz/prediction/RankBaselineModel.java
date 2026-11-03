package com.example.predictteniz.prediction;


/** Deterministic fixture for bracket mathematics. Production models run in Python. */
public class RankBaselineModel implements WinProbabilityModel {
    @Override
    public String version() { return "ranking-baseline-v1"; }

    @Override
    public double probability(int rankA, int rankB) {
        if (rankA < 1 || rankB < 1) throw new IllegalArgumentException("Ranks must be positive.");
        return Math.sqrt(rankB) / (Math.sqrt(rankA) + Math.sqrt(rankB));
    }
}
