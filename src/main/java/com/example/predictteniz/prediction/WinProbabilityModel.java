package com.example.predictteniz.prediction;

public interface WinProbabilityModel {
    String version();
    double probability(int rankA, int rankB);

    default double probability(int rankA, int rankB, MatchConditions conditions) {
        return probability(rankA, rankB);
    }

    default double probability(MatchPair match) {
        return probability(match.rankA(), match.rankB(), match.conditions());
    }
}
