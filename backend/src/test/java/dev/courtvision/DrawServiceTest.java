package dev.courtvision;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DrawServiceTest {
    private DrawService equalPlayers() {
        PlayerRepository repository = mock(PlayerRepository.class);
        when(repository.require(anyString())).thenAnswer(call -> new Player(call.getArgument(0), call.getArgument(0), "POL", 2000, 2000, 2000));
        return new DrawService(repository, new PredictionService());
    }
    private DrawRequest request(List<String> ids, List<Long> prizes) {
        return new DrawRequest("Test", Surface.HARD, ids, prizes, "EUR");
    }
    @Test void equalFourPlayerDrawHasExactProbabilitiesAndPrizeExpectation() {
        var result = equalPlayers().calculate(request(List.of("a","b","c","d"), List.of(100L,200L,400L)));
        assertThat(result.standings()).allSatisfy(s -> {
            assertThat(s.advancement()).containsExactly(0.5, 0.25);
            assertThat(s.expectedPrize()).isEqualTo(200);
        });
        assertThat(result.standings().stream().mapToDouble(DrawResult.Standing::titleProbability).sum()).isCloseTo(1, within(1e-12));
    }
    @Test void rejectsDuplicatePlayersAndWrongPrizeSchedule() {
        assertThatThrownBy(() -> equalPlayers().calculate(request(List.of("a","a"), List.of(1L,2L)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> equalPlayers().calculate(request(List.of("a","b","c"), List.of(1L,2L)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> equalPlayers().calculate(request(List.of("a","b"), List.of(2L,1L)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> equalPlayers().calculate(request(List.of("a","b"), List.of(1L,2L,3L)))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void probabilitiesAreSymmetricAndSurfaceSensitive() {
        var service = new PredictionService();
        var a = new Player("a","A","POL",2200,1900,2000);
        var b = new Player("b","B","ITA",2000,2100,2000);
        assertThat(service.probability(a,b,Surface.HARD) + service.probability(b,a,Surface.HARD)).isCloseTo(1,within(1e-12));
        assertThat(service.probability(a,b,Surface.HARD)).isGreaterThan(0.5);
        assertThat(service.probability(a,b,Surface.CLAY)).isLessThan(0.5);
    }
    @Test void exactEngineAgreesWithExhaustiveEnumerationForFourPlayers() {
        var prediction = new PredictionService();
        var a = new Player("a","A","POL",2200,2200,2200);
        var b = new Player("b","B","ITA",2000,2000,2000);
        var c = new Player("c","C","ESP",2100,2100,2100);
        var d = new Player("d","D","USA",1900,1900,1900);
        PlayerRepository repository = mock(PlayerRepository.class);
        for (var p : List.of(a,b,c,d)) when(repository.require(p.id())).thenReturn(p);
        var result = new DrawService(repository,prediction).calculate(request(List.of("a","b","c","d"),List.of(100L,200L,400L)));
        double expected = prediction.probability(a,b,Surface.HARD) *
            (prediction.probability(c,d,Surface.HARD)*prediction.probability(a,c,Surface.HARD) +
             prediction.probability(d,c,Surface.HARD)*prediction.probability(a,d,Surface.HARD));
        assertThat(result.standings().stream().filter(s -> s.player().id().equals("a")).findFirst().orElseThrow().titleProbability()).isCloseTo(expected,within(1e-12));
        double totalPrizes = result.standings().stream().mapToDouble(DrawResult.Standing::expectedPrize).sum();
        assertThat(totalPrizes).isCloseTo(800, within(1e-9));
    }
}
