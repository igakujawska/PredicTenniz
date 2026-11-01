package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.PredictionService.RankedPlayer;
import com.example.predictteniz.prediction.RankBaselineModel;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BracketCalculatorTest {
    private final BracketCalculator calculator = new BracketCalculator(new RankBaselineModel());

    private RankedPlayer player(long id, int rank) {
        return new RankedPlayer(id, "Player " + id, "POL", rank, LocalDate.of(2026, 6, 8));
    }

    @Test
    void fourEqualPlayersEachHaveOneQuarterChanceAndPairsStayInOrder() {
        var result = calculator.calculate(List.of(player(1, 5), player(2, 5), player(3, 5), player(4, 5)));
        assertThat(result.titleChances()).allSatisfy(chance -> assertThat(chance.probability()).isEqualTo(0.25));
        assertThat(result.rounds()).hasSize(2);
        assertThat(result.rounds().getFirst().matches().getFirst().playerB().id()).isEqualTo(2);
        assertThat(result.rounds().getLast().matches().getFirst().playerB().id()).isEqualTo(3);
    }

    @Test
    void titleChanceIncludesEveryPossibleFinalOpponent() {
        var result = calculator.calculate(List.of(player(1, 1), player(2, 4), player(3, 9), player(4, 16)));
        var first = result.titleChances().stream().filter(chance -> chance.player().id() == 1).findFirst().orElseThrow();
        assertThat(first.probability()).isCloseTo(0.5142857142857142, within(1e-12));
        assertThat(result.titleChances().stream().mapToDouble(BracketCalculator.TitleChance::probability).sum())
            .isCloseTo(1, within(1e-12));
    }

    @Test
    void sixteenPlayersProduceFourRoundsAndNormalizedProbabilities() {
        var result = calculator.calculate(IntStream.rangeClosed(1, 16).mapToObj(i -> player(i, i)).toList());
        assertThat(result.rounds()).hasSize(4);
        assertThat(result.titleChances()).hasSize(16);
        assertThat(result.titleChances().stream().mapToDouble(BracketCalculator.TitleChance::probability).sum())
            .isCloseTo(1, within(1e-12));
    }

    @Test
    void unsupportedSizeAndDuplicatePlayersAreRejected() {
        assertThatThrownBy(() -> calculator.calculate(List.of(player(1, 1), player(2, 2), player(3, 3))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(List.of(player(1, 1), player(1, 1))))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
