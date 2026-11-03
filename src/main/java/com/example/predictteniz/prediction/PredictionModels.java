package com.example.predictteniz.prediction;

import com.example.predictteniz.prediction.ml.CachedPairModel;
import com.example.predictteniz.prediction.ml.MlClient;
import com.example.predictteniz.prediction.ml.MlContract;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PredictionModels {
    private final MlClient client;

    public WinProbabilityModel prepare(String version, LocalDate asOf, List<PredictionService.RankedPlayer> players,
                                       MatchConditions conditions) {
        String selected = version == null ? "ranking-baseline-v1" : version;
        var pairs = new ArrayList<MlContract.Pair>();
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                var a = players.get(i);
                var b = players.get(j);
                pairs.add(new MlContract.Pair(a.id(), b.id(), a.position(), b.position(), a.sourceId(), b.sourceId()));
            }
        }
        var request = new MlContract.Batch(selected, asOf, conditions.surface().name(), conditions.bestOf(), List.copyOf(pairs), conditions.category());
        return CachedPairModel.from(request, client.predict(request));
    }

    public List<MlContract.ModelInfo> descriptions() { return client.models(); }
}
