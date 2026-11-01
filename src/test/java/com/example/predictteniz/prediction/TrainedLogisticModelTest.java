package com.example.predictteniz.prediction;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class TrainedLogisticModelTest {
    private final TrainedLogisticModel model = new TrainedLogisticModel();

    @Test
    void javaInferenceMatchesProbabilitiesExportedByPython() throws Exception {
        try (var input = new ClassPathResource("models/rank-logistic-v1.json").getInputStream()) {
            Map<?, ?> artifact = JsonMapper.builder().build().readValue(input, Map.class);
            for (Object item : (List<?>) artifact.get("parityCases")) {
                var fixture = (Map<?, ?>) item;
                var conditions = new MatchConditions(MatchConditions.Surface.valueOf((String) fixture.get("surface")),
                    ((Number) fixture.get("bestOf")).intValue());
                double actual = model.probability(((Number) fixture.get("rankA")).intValue(),
                    ((Number) fixture.get("rankB")).intValue(), conditions);
                assertThat(actual).isCloseTo(((Number) fixture.get("probability")).doubleValue(), within(1e-12));
            }
        }
    }

    @Test
    void changingPlayerOrderComplementsProbabilityForEverySurfaceAndFormat() {
        for (var surface : MatchConditions.Surface.values()) {
            for (int bestOf : new int[]{3, 5}) {
                var conditions = new MatchConditions(surface, bestOf);
                assertThat(model.probability(2, 17, conditions) + model.probability(17, 2, conditions))
                    .isCloseTo(1, within(1e-12));
                assertThat(model.probability(10, 10, conditions)).isEqualTo(0.5);
            }
        }
    }

    @Test
    void unsupportedInputsAreRejected() {
        assertThatThrownBy(() -> new MatchConditions(MatchConditions.Surface.HARD, 4))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> model.probability(0, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
