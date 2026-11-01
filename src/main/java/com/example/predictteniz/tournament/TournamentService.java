package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.PredictionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TournamentService {
    private final TournamentDrawRepository draws;
    private final PredictionService predictions;
    private final BracketCalculator calculator;

    public TournamentService(TournamentDrawRepository draws, PredictionService predictions, BracketCalculator calculator) {
        this.draws = draws;
        this.predictions = predictions;
        this.calculator = calculator;
    }

    public DrawResponse preview(String title, LocalDate asOf, List<Long> playerIds) {
        validate(title, asOf, playerIds);
        var forecast = calculator.calculate(predictions.snapshots(playerIds, asOf));
        return new DrawResponse(null, title.strip(), asOf, null, forecast);
    }

    @Transactional
    public DrawResponse create(String title, LocalDate asOf, List<Long> playerIds) {
        var preview = preview(title, asOf, playerIds);
        var draw = draws.save(new TournamentDraw(title.strip(), asOf, playerIds));
        return new DrawResponse(draw.getId(), draw.getTitle(), draw.getAsOf(), draw.getCreatedAt(), preview.forecast());
    }

    public DrawResponse get(UUID id) {
        var draw = draws.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono drabinki."));
        var forecast = calculator.calculate(predictions.snapshots(draw.getPlayerIds(), draw.getAsOf()));
        return new DrawResponse(draw.getId(), draw.getTitle(), draw.getAsOf(), draw.getCreatedAt(), forecast);
    }

    public List<DrawSummary> recent() {
        return draws.findTop20ByOrderByCreatedAtDesc().stream().map(draw ->
            new DrawSummary(draw.getId(), draw.getTitle(), draw.getAsOf(), draw.getPlayerIds().size())).toList();
    }

    private void validate(String title, LocalDate asOf, List<Long> ids) {
        if (title == null || title.isBlank() || title.strip().length() > 100 || asOf == null
                || asOf.isAfter(LocalDate.now()) || ids == null || ids.size() < 2 || ids.size() > 16
                || (ids.size() & (ids.size() - 1)) != 0 || ids.stream().anyMatch(id -> id == null || id <= 0)
                || ids.stream().distinct().count() != ids.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Podaj nazwę, datę danych do dziś i 2, 4, 8 lub 16 różnych zawodników.");
    }

    public record DrawResponse(UUID id, String title, LocalDate asOf, Instant createdAt,
                               BracketCalculator.Forecast forecast) {}
    public record DrawSummary(UUID id, String title, LocalDate asOf, int players) {}
}
