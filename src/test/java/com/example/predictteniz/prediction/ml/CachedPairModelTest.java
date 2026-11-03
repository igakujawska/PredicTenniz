package com.example.predictteniz.prediction.ml;

import com.example.predictteniz.prediction.MatchConditions;
import com.example.predictteniz.prediction.MatchPair;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class CachedPairModelTest {
    private final MlContract.Batch request = new MlContract.Batch("model", LocalDate.of(2026, 6, 8), "HARD", 3,
        List.of(new MlContract.Pair(10, 20, 1, 1, null, null), new MlContract.Pair(10, 30, 1, 1, null, null)));

    private MlContract.BatchResult result(List<MlContract.Outcome> predictions) {
        return new MlContract.BatchResult("model", request.asOf(), "HARD", 3, predictions);
    }

    @Test
    void usesPlayerIdentityEvenWhenRanksAreEqualAndResponsesAreReordered() {
        var model = CachedPairModel.from(request, result(List.of(
            new MlContract.Outcome(30, 10, 0.3, 0.7), new MlContract.Outcome(10, 20, 0.8, 0.2))));
        assertThat(model.probability(new MatchPair(10, 20, 1, 1, MatchConditions.DEFAULT))).isEqualTo(0.8);
        assertThat(model.probability(new MatchPair(20, 10, 1, 1, MatchConditions.DEFAULT))).isCloseTo(0.2, within(1e-12));
        assertThat(model.probability(new MatchPair(10, 30, 1, 1, MatchConditions.DEFAULT))).isEqualTo(0.7);
    }

    @Test
    void rejectsMissingDuplicateOrUnnormalizedPredictions() {
        assertThatThrownBy(() -> CachedPairModel.from(request, result(List.of(new MlContract.Outcome(10, 20, 0.8, 0.2)))))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> CachedPairModel.from(request, result(List.of(
            new MlContract.Outcome(10, 20, 0.8, 0.2), new MlContract.Outcome(10, 20, 0.8, 0.2)))))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> CachedPairModel.from(request, result(List.of(
            new MlContract.Outcome(10, 20, 0.8, 0.8), new MlContract.Outcome(10, 30, 0.7, 0.3)))))
            .isInstanceOf(ResponseStatusException.class);
    }
}
