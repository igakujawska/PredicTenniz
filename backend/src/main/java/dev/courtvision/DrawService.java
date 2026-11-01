package dev.courtvision;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DrawService {
    private final PlayerRepository players;
    private final PredictionService predictions;
    public DrawService(PlayerRepository players, PredictionService predictions) {
        this.players = players;
        this.predictions = predictions;
    }

    public DrawResult calculate(DrawRequest request) {
        int n = request.playerIds().size();
        if (n < 2 || n > 64 || (n & (n-1)) != 0)
            throw new IllegalArgumentException("Draw size must be 2, 4, 8, 16, 32 or 64.");
        if (new HashSet<>(request.playerIds()).size() != n)
            throw new IllegalArgumentException("A player cannot appear twice in a draw.");
        int rounds = Integer.numberOfTrailingZeros(n);
        if (request.prizes().size() != rounds + 1)
            throw new IllegalArgumentException("Provide one total prize per exit round and one for the champion.");
        for (int r = 1; r < request.prizes().size(); r++)
            if (request.prizes().get(r) < request.prizes().get(r-1))
                throw new IllegalArgumentException("Total prizes must be in ascending order.");

        List<Player> draw = request.playerIds().stream().map(players::require).toList();
        double[][] q = new double[n][rounds+1];
        for (int i = 0; i < n; i++) q[i][0] = 1;
        for (int r = 1; r <= rounds; r++) {
            int block = 1 << r;
            int half = block / 2;
            for (int i = 0; i < n; i++) {
                int start = (i / block) * block;
                int opponentStart = i < start + half ? start + half : start;
                double opponentSum = 0;
                for (int j = opponentStart; j < opponentStart + half; j++)
                    opponentSum += q[j][r-1] * predictions.probability(draw.get(i), draw.get(j), request.surface());
                q[i][r] = q[i][r-1] * opponentSum;
            }
        }

        List<DrawResult.Standing> standings = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double expected = q[i][rounds] * request.prizes().get(rounds);
            List<Double> advancement = new ArrayList<>();
            for (int r = 0; r < rounds; r++) {
                expected += (q[i][r] - q[i][r+1]) * request.prizes().get(r);
                advancement.add(q[i][r+1]);
            }
            standings.add(new DrawResult.Standing(draw.get(i), advancement, q[i][rounds], expected));
        }
        standings.sort(Comparator.comparingDouble(DrawResult.Standing::titleProbability).reversed());

        List<List<DrawResult.Match>> bracket = new ArrayList<>();
        List<Player> remaining = draw;
        while (remaining.size() > 1) {
            List<DrawResult.Match> matches = new ArrayList<>();
            List<Player> winners = new ArrayList<>();
            for (int i = 0; i < remaining.size(); i += 2) {
                Player a = remaining.get(i), b = remaining.get(i+1);
                double p = predictions.probability(a, b, request.surface());
                Player winner = p >= 0.5 ? a : b;
                matches.add(new DrawResult.Match(a.id(), b.id(), winner.id(), p));
                winners.add(winner);
            }
            bracket.add(matches);
            remaining = winners;
        }
        return new DrawResult(UUID.randomUUID().toString(), request.title().strip(), request.surface(), request.currency(),
            request.prizes(), Instant.now().toString(), PredictionService.MODEL_VERSION, "ILLUSTRATIVE", standings, bracket);
    }
}
