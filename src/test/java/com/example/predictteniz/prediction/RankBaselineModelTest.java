package com.example.predictteniz.prediction;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RankBaselineModelTest {
    private final WinProbabilityModel model = new RankBaselineModel();

    @Test
    void equalRankIsFairAndReversingPlayersComplementsProbability() {
        assertThat(model.probability(10, 10)).isEqualTo(0.5);
        assertThat(model.probability(1, 16)).isCloseTo(0.8, within(1e-12));
        assertThat(model.probability(1, 16) + model.probability(16, 1)).isCloseTo(1, within(1e-12));
        assertThat(model.probability(1, Integer.MAX_VALUE)).isBetween(0.5, 1.0);
    }

    @Test
    void invalidRanksAreRejected() {
        assertThatThrownBy(() -> model.probability(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> model.probability(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
