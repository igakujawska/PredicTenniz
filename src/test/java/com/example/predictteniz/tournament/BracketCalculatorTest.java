package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.PredictionService.RankedPlayer;
import com.example.predictteniz.prediction.RankBaselineModel;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BracketCalculatorTest {
    private final BracketCalculator calculator = new BracketCalculator();

    private BracketCalculator.Forecast calculate(List<RankedPlayer> players) {
        return calculator.calculate(players, new RankBaselineModel(), com.example.predictteniz.prediction.MatchConditions.DEFAULT);
    }

    private RankedPlayer player(long id, int rank) {
        return new RankedPlayer(id, "Player " + id, "POL", rank, LocalDate.of(2026, 6, 8));
    }

    @Test
    void fourEqualPlayersEachHaveOneQuarterChanceAndPairsStayInOrder() {
        var result = calculate(List.of(player(1, 5), player(2, 5), player(3, 5), player(4, 5)));
        assertThat(result.titleChances()).allSatisfy(chance -> assertThat(chance.probability()).isEqualTo(0.25));
        assertThat(result.rounds()).hasSize(2);
        assertThat(result.rounds().getFirst().matches().getFirst().playerB().id()).isEqualTo(2);
        assertThat(result.rounds().getLast().matches().getFirst().playerB().id()).isEqualTo(3);
    }

    @Test
    void titleChanceIncludesEveryPossibleFinalOpponent() {
        var result = calculate(List.of(player(1, 1), player(2, 4), player(3, 9), player(4, 16)));
        var first = result.titleChances().stream().filter(chance -> chance.player().id() == 1).findFirst().orElseThrow();
        assertThat(first.probability()).isCloseTo(0.5142857142857142, within(1e-12));
        assertThat(result.titleChances().stream().mapToDouble(BracketCalculator.TitleChance::probability).sum())
            .isCloseTo(1, within(1e-12));
    }

    @Test
    void sixteenPlayersProduceFourRoundsAndNormalizedProbabilities() {
        var result = calculate(IntStream.rangeClosed(1, 16).mapToObj(i -> player(i, i)).toList());
        assertThat(result.rounds()).hasSize(4);
        assertThat(result.titleChances()).hasSize(16);
        assertThat(result.titleChances().stream().mapToDouble(BracketCalculator.TitleChance::probability).sum())
            .isCloseTo(1, within(1e-12));
    }

    @Test
    void unsupportedSizeAndDuplicatePlayersAreRejected() {
        assertThatThrownBy(() -> calculate(List.of(player(1, 1), player(2, 2), player(3, 3))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculate(List.of(player(1, 1), player(1, 1))))
            .isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void byesAdvanceWithoutCallingTheModelAndDoNotCountAsMatches() {
        var pairCalls = new java.util.concurrent.atomic.AtomicInteger();
        var model = new com.example.predictteniz.prediction.WinProbabilityModel() {
            public String version() { return "test"; }
            public double probability(int a, int b) { pairCalls.incrementAndGet(); return .5; }
        };
        var slots = java.util.Arrays.asList(player(1,1), null, player(2,1), player(3,1));
        var result = calculator.calculate(slots,model,com.example.predictteniz.prediction.MatchConditions.DEFAULT);
        assertThat(result.rounds().getFirst().matches().getFirst().bye()).isTrue();
        assertThat(result.titleChances().stream().filter(c -> c.player().id()==1).findFirst().orElseThrow().probability()).isEqualTo(.5);
        assertThat(result.rounds().stream().flatMap(r -> r.matches().stream()).filter(m -> !m.bye())).hasSize(2);
        assertThat(pairCalls.get()).isPositive();
    }

    @Test
    void fullGrandSlamHasSevenRoundsAndOneHundredTwentySevenMatches() {
        var result = calculate(IntStream.rangeClosed(1,128).mapToObj(i -> player(i,10)).toList());
        assertThat(result.rounds()).hasSize(7);
        assertThat(result.rounds().stream().mapToInt(r -> r.matches().size()).sum()).isEqualTo(127);
        assertThat(result.titleChances()).allSatisfy(c -> assertThat(c.probability()).isEqualTo(1.0/128));
        assertThatThrownBy(() -> calculate(java.util.Arrays.asList(null,null,player(1,1),player(2,1))))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
