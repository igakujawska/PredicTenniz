package com.example.predictteniz.prediction;

public record MatchPair(long playerAId, long playerBId, int rankA, int rankB, MatchConditions conditions) {}
