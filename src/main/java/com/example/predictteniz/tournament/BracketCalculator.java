package com.example.predictteniz.tournament;

import lombok.RequiredArgsConstructor;

import com.example.predictteniz.prediction.MatchConditions;
import com.example.predictteniz.prediction.PredictionService.RankedPlayer;
import com.example.predictteniz.prediction.WinProbabilityModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BracketCalculator {
    private final WinProbabilityModel model;

    public Forecast calculate(List<RankedPlayer> players) {
        return calculate(players, model, MatchConditions.DEFAULT);
    }

    public Forecast calculate(List<RankedPlayer> players, WinProbabilityModel selected, MatchConditions conditions) {
        int size = players.size();
        if (size < 2 || size > 16 || (size & (size - 1)) != 0
                || players.stream().map(RankedPlayer::id).distinct().count() != size)
            throw new IllegalArgumentException("Draw needs 2, 4, 8 or 16 different players.");
        var byId = players.stream().collect(Collectors.toMap(RankedPlayer::id, Function.identity()));
        List<Map<Long, Double>> distributions = players.stream()
            .map(player -> Map.of(player.id(), 1.0)).toList();
        List<RankedPlayer> projected = players;
        List<Round> rounds = new ArrayList<>();
        int round = 1;
        while (distributions.size() > 1) {
            var next = new ArrayList<Map<Long, Double>>();
            var nextProjected = new ArrayList<RankedPlayer>();
            var matches = new ArrayList<ProjectedMatch>();
            for (int i = 0; i < distributions.size(); i += 2) {
                Map<Long, Double> winners = new LinkedHashMap<>();
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
                double p = probability(a, b, selected, conditions);
                RankedPlayer winner = p >= 0.5 ? a : b;
                matches.add(new ProjectedMatch(a, b, p, 1 - p, winner.id()));
                nextProjected.add(winner);
            }
            String name = switch (distributions.size()) {
                case 2 -> "Finał";
                case 4 -> "Półfinały";
                case 8 -> "Ćwierćfinały";
                default -> "1/8 finału";
            };
            rounds.add(new Round(round++, name, List.copyOf(matches)));
            distributions = next;
            projected = nextProjected;
        }
        var chances = distributions.getFirst().entrySet().stream()
            .map(entry -> new TitleChance(byId.get(entry.getKey()), entry.getValue()))
            .sorted(Comparator.comparingDouble(TitleChance::probability).reversed()
                .thenComparingLong(chance -> chance.player().id())).toList();
        return new Forecast(selected.version(), conditions.surface(), conditions.bestOf(), List.copyOf(rounds), chances);
    }

    private double probability(RankedPlayer a, RankedPlayer b, WinProbabilityModel selected, MatchConditions conditions) {
        double probability = selected.probability(a.position(), b.position(), conditions);
        if (!Double.isFinite(probability) || probability < 0 || probability > 1)
            throw new IllegalStateException("Model returned an invalid probability.");
        return probability;
    }

    public record Forecast(String modelVersion, MatchConditions.Surface surface, int bestOf, List<Round> rounds, List<TitleChance> titleChances) {}
    public record Round(int number, String name, List<ProjectedMatch> matches) {}
    public record ProjectedMatch(RankedPlayer playerA, RankedPlayer playerB, double probabilityA,
                                 double probabilityB, long predictedWinnerId) {}
    public record TitleChance(RankedPlayer player, double probability) {}
}
