package com.example.predictteniz.prediction.ml;

import com.example.predictteniz.prediction.MatchPair;
import com.example.predictteniz.prediction.WinProbabilityModel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CachedPairModel implements WinProbabilityModel {
    @Getter @Accessors(fluent = true)
    private final String version;
    private final Map<Key, Double> probabilities;

    public static CachedPairModel from(MlContract.Batch request, MlContract.BatchResult result) {
        if (result == null || result.predictions() == null || !request.modelVersion().equals(result.modelVersion())
                || result.predictions().size() != request.pairs().size()) throw invalid();
        Set<Key> expected = new HashSet<>();
        request.pairs().forEach(pair -> expected.add(Key.of(pair.playerAId(), pair.playerBId())));
        Map<Key, Double> probabilities = new HashMap<>();
        for (var prediction : result.predictions()) {
            if (prediction == null) throw invalid();
            var key = Key.of(prediction.playerAId(), prediction.playerBId());
            double a = prediction.probabilityA(), b = prediction.probabilityB();
            if (!expected.contains(key) || probabilities.containsKey(key) || !Double.isFinite(a) || !Double.isFinite(b)
                    || a < 0 || a > 1 || b < 0 || b > 1 || Math.abs(a + b - 1) > 1e-9) throw invalid();
            probabilities.put(key, prediction.playerAId() == key.low() ? a : b);
        }
        if (probabilities.size() != expected.size()) throw invalid();
        return new CachedPairModel(request.modelVersion(), Map.copyOf(probabilities));
    }

    @Override
    public double probability(MatchPair match) {
        var key = Key.of(match.playerAId(), match.playerBId());
        Double probability = probabilities.get(key);
        if (probability == null) throw invalid();
        return match.playerAId() == key.low() ? probability : 1 - probability;
    }

    @Override
    public double probability(int rankA, int rankB) {
        throw new UnsupportedOperationException("Remote predictions are keyed by player IDs, not ranks.");
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Silnik ML zwrócił niekompletną lub niespójną macierz predykcji.");
    }

    private record Key(long low, long high) {
        static Key of(long a, long b) { return new Key(Math.min(a, b), Math.max(a, b)); }
    }
}
