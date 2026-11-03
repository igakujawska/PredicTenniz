package com.example.predictteniz.tournament;


import com.example.predictteniz.prediction.MatchConditions;
import com.example.predictteniz.prediction.MatchPair;
import com.example.predictteniz.prediction.PredictionService.RankedPlayer;
import com.example.predictteniz.prediction.WinProbabilityModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BracketCalculator {
    public Forecast calculate(List<RankedPlayer> players, WinProbabilityModel selected, MatchConditions conditions) {
        int size = players.size();
        if (size < 2 || size > 128 || (size & (size - 1)) != 0
                || players.stream().filter(Objects::nonNull).map(RankedPlayer::id).distinct().count() != players.stream().filter(Objects::nonNull).count())
            throw new IllegalArgumentException("Draw needs 2 to 128 slots, unique players and no empty first-round matches.");
        if (players.stream().filter(Objects::nonNull).count() < 2) throw new IllegalArgumentException("At least two players are required.");
        for (int slot = 0; slot < size; slot += 2)
            if (players.get(slot) == null && players.get(slot + 1) == null) throw new IllegalArgumentException("Two byes cannot face each other.");
        var byId = players.stream().filter(Objects::nonNull).collect(Collectors.toMap(RankedPlayer::id, Function.identity()));
        List<Map<Long, Double>> distributions = players.stream()
            .map(player -> player == null ? Map.<Long, Double>of() : Map.of(player.id(), 1.0)).toList();
        List<RankedPlayer> projected = players;
        List<Round> rounds = new ArrayList<>();
        int round = 1;
        while (distributions.size() > 1) {
            var next = new ArrayList<Map<Long, Double>>();
            var nextProjected = new ArrayList<RankedPlayer>();
            var matches = new ArrayList<ProjectedMatch>();
            for (int i = 0; i < distributions.size(); i += 2) {
                Map<Long, Double> winners = new LinkedHashMap<>();
                if (distributions.get(i).isEmpty()) winners.putAll(distributions.get(i + 1));
                if (distributions.get(i + 1).isEmpty()) winners.putAll(distributions.get(i));
                for (var left : distributions.get(i).entrySet()) {
                    for (var right : distributions.get(i + 1).entrySet()) {
                        double encounter = left.getValue() * right.getValue();
                        double probability = probability(byId.get(left.getKey()), byId.get(right.getKey()), selected, conditions);
                        winners.merge(left.getKey(), encounter * probability, Double::sum);
                        winners.merge(right.getKey(), encounter * (1 - probability), Double::sum);
                    }
                }
                next.add(winners);
                RankedPlayer a = projected.get(i);
                RankedPlayer b = projected.get(i + 1);
                double p = a == null ? 0 : b == null ? 1 : probability(a, b, selected, conditions);
                RankedPlayer winner = p >= 0.5 ? a : b;
                matches.add(new ProjectedMatch(a, b, p, 1 - p, winner.id(), a == null || b == null));
                nextProjected.add(winner);
            }
            String name = switch (distributions.size()) {
                case 2 -> "Finał";
                case 4 -> "Półfinały";
                case 8 -> "Ćwierćfinały";
                default -> "Runda " + distributions.size();
            };
            rounds.add(new Round(round++, name, List.copyOf(matches)));
            distributions = next;
            projected = nextProjected;
        }
        var chances = distributions.getFirst().entrySet().stream()
            .map(entry -> new TitleChance(byId.get(entry.getKey()), entry.getValue()))
            .sorted(Comparator.comparingDouble(TitleChance::probability).reversed()
                .thenComparingLong(chance -> chance.player().id())).toList();
        return new Forecast(selected.version(), conditions.surface(), conditions.bestOf(), List.copyOf(rounds), chances, conditions.category());
    }

    private double probability(RankedPlayer a, RankedPlayer b, WinProbabilityModel selected, MatchConditions conditions) {
        double probability = selected.probability(new MatchPair(a.id(), b.id(), a.position(), b.position(), conditions));
        if (!Double.isFinite(probability) || probability < 0 || probability > 1)
            throw new IllegalStateException("Model returned an invalid probability.");
        return probability;
    }

    public record Forecast(String modelVersion, MatchConditions.Surface surface, int bestOf, List<Round> rounds, List<TitleChance> titleChances, String category) {}
    public record Round(int number, String name, List<ProjectedMatch> matches) {}
    public record ProjectedMatch(RankedPlayer playerA, RankedPlayer playerB, double probabilityA,
                                 double probabilityB, long predictedWinnerId, boolean bye) {}
    public record TitleChance(RankedPlayer player, double probability) {}
}
