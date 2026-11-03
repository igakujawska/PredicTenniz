package com.example.predictteniz.tournament;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Men's singles reference formats, not live entry lists. Sources: docs/TOURNAMENTS.md. */
@Getter
@RequiredArgsConstructor
public enum TournamentFormat {
    CUSTOM("Własna drabinka", "TOUR", 0, 0, 0, 0, 0),
    ATP250_28("ATP 250 · 28 zawodników", "ATP_250", 28, 32, 8, 3, 250),
    ATP250_32("ATP 250 · 32 zawodników", "ATP_250", 32, 32, 8, 3, 250),
    ATP250_48("ATP 250 · 48 zawodników", "ATP_250", 48, 64, 16, 3, 250),
    ATP500_32("ATP 500 · 32 zawodników", "ATP_500", 32, 32, 8, 3, 500),
    ATP500_48("ATP 500 · 48 zawodników", "ATP_500", 48, 64, 16, 3, 500),
    MASTERS_56("Masters 1000 · 56 zawodników", "MASTERS_1000", 56, 64, 16, 3, 1000),
    MASTERS_96("Masters 1000 · 96 zawodników", "MASTERS_1000", 96, 128, 32, 3, 1000),
    SLAM_128("Wielki Szlem · 128 zawodników", "GRAND_SLAM", 128, 128, 32, 5, 2000);

    private final String label;
    private final String category;
    private final int entrants;
    private final int drawSize;
    private final int seeds;
    private final int bestOf;
    private final int championPoints;

    public int getByes() { return drawSize - entrants; }
    public Description describe() { return new Description(name(), label, category, entrants, drawSize, getByes(), seeds, bestOf, championPoints); }
    public static List<Description> descriptions() { return Arrays.stream(values()).map(TournamentFormat::describe).toList(); }
    public record Description(String code, String label, String category, int entrants, int drawSize,
                              int byes, int seeds, int bestOf, int championPoints) {}
}
