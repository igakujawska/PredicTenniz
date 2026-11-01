package dev.courtvision;

import java.util.List;

public record DrawResult(String id, String title, Surface surface, String currency, List<Long> prizes,
                         String createdAt, String modelVersion, String dataMode, List<Standing> standings,
                         List<List<Match>> bracket) {
    public record Standing(Player player, List<Double> advancement, double titleProbability, double expectedPrize) {}
    public record Match(String firstId, String secondId, String winnerId, double firstProbability) {}
}
