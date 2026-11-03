package com.example.predictteniz.tournament;

import lombok.RequiredArgsConstructor;
import lombok.Builder;
import com.example.predictteniz.prediction.MatchConditions;
import com.example.predictteniz.prediction.PredictionModels;
import com.example.predictteniz.prediction.PredictionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class TournamentService {
    private final TournamentDrawStore store;
    private final PredictionService predictions;
    private final BracketCalculator calculator;
    private final PredictionModels models;

    public DrawResponse preview(String title, LocalDate asOf, List<Long> playerIds,
                                MatchConditions.Surface surface, Integer bestOf, String version, TournamentFormat formatCode) {
        var format = formatCode == null ? TournamentFormat.CUSTOM : formatCode;
        validate(title, asOf, playerIds, format);
        var conditions = conditions(surface, bestOf, format);
        var snapshots = predictions.snapshots(playerIds.stream().filter(Objects::nonNull).toList(), asOf);
        var byId = snapshots.stream().collect(Collectors.toMap(PredictionService.RankedPlayer::id, Function.identity()));
        var slots = playerIds.stream().map(id -> id == null ? null : byId.get(id)).toList();
        var model = models.prepare(version, asOf, snapshots, conditions);
        var forecast = calculator.calculate(slots, model, conditions);
        return DrawResponse.builder().title(title.strip()).asOf(asOf)
            .playerIds(java.util.Collections.unmodifiableList(new java.util.ArrayList<>(playerIds)))
            .surface(conditions.surface()).bestOf(conditions.bestOf()).modelVersion(forecast.modelVersion())
            .formatCode(format).formatInfo(format.describe()).forecast(forecast).build();
    }

    public DrawResponse create(String title, LocalDate asOf, List<Long> playerIds,
                               MatchConditions.Surface surface, Integer bestOf, String version, TournamentFormat format) {
        var preview = preview(title, asOf, playerIds, surface, bestOf, version, format);
        var saved = store.create(preview.title(), asOf, playerIds, conditions(surface, bestOf, preview.formatCode()), preview.modelVersion(), preview.formatCode());
        return response(saved, preview.forecast(), null);
    }

    public DrawResponse update(UUID id, Long expectedVersion, String title, LocalDate asOf, List<Long> playerIds,
                               MatchConditions.Surface surface, Integer bestOf, String version, TournamentFormat format) {
        var preview = preview(title, asOf, playerIds, surface, bestOf, version, format);
        var saved = store.update(id, expectedVersion, preview.title(), asOf, playerIds, conditions(surface, bestOf, preview.formatCode()), preview.modelVersion(), preview.formatCode());
        return response(saved, preview.forecast(), null);
    }

    public DrawResponse get(UUID id) {
        var draw = store.get(id);
        try {
            var preview = preview(draw.title(), draw.asOf(), draw.playerIds(), draw.conditions().surface(), draw.conditions().bestOf(), draw.modelVersion(), draw.formatCode());
            return response(draw, preview.forecast(), null);
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() < 500) throw error;
            return response(draw, null, error.getReason());
        }
    }

    public List<DrawSummary> recent() { return store.recent(); }

    private DrawResponse response(TournamentDrawStore.StoredDraw draw, BracketCalculator.Forecast forecast, String error) {
        return DrawResponse.builder().id(draw.id()).title(draw.title()).asOf(draw.asOf()).createdAt(draw.createdAt())
            .version(draw.version()).playerIds(draw.playerIds()).surface(draw.conditions().surface()).bestOf(draw.conditions().bestOf())
            .formatCode(draw.formatCode()).formatInfo(draw.formatCode().describe())
            .modelVersion(draw.modelVersion()).forecast(forecast).predictionError(error).build();
    }

    private MatchConditions conditions(MatchConditions.Surface surface, Integer bestOf, TournamentFormat format) {
        int expected = format == TournamentFormat.CUSTOM ? 3 : format.getBestOf();
        int chosen = bestOf == null ? expected : bestOf;
        if ((chosen != 3 && chosen != 5) || (format != TournamentFormat.CUSTOM && chosen != expected))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format meczu nie pasuje do wybranego turnieju.");
        return new MatchConditions(surface == null ? MatchConditions.Surface.HARD : surface, chosen, format.getCategory());
    }

    private void validate(String title, LocalDate asOf, List<Long> ids, TournamentFormat format) {
        if (title == null || title.isBlank() || title.strip().length() > 100 || asOf == null || asOf.isAfter(LocalDate.now())
                || ids == null || ids.size() < 2 || ids.size() > 128 || (ids.size() & (ids.size()-1)) != 0)
            throw invalid("Podaj nazwę, datę do dziś i drabinkę z 2–128 miejscami (potęga dwójki).");
        var entrants = ids.stream().filter(Objects::nonNull).toList();
        if (entrants.size() < 2 || entrants.stream().anyMatch(id -> id <= 0) || entrants.stream().distinct().count() != entrants.size())
            throw invalid("Wybierz różnych zawodników; wolny los oznacz jako null.");
        for (int i=0; i<ids.size(); i+=2)
            if (ids.get(i)==null && ids.get(i+1)==null) throw invalid("Dwa wolne losy nie mogą tworzyć pary.");
        if (format != TournamentFormat.CUSTOM && (ids.size()!=format.getDrawSize() || entrants.size()!=format.getEntrants()))
            throw invalid("Liczba graczy i wolnych losów nie pasuje do wybranego formatu turnieju.");
    }

    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    @Builder
    public record DrawResponse(UUID id, String title, LocalDate asOf, Instant createdAt, long version,
                               List<Long> playerIds, MatchConditions.Surface surface, int bestOf, String modelVersion,
                               TournamentFormat formatCode, TournamentFormat.Description formatInfo,
                               BracketCalculator.Forecast forecast, String predictionError) {}
    public record DrawSummary(UUID id, String title, LocalDate asOf, int players) {}
}
