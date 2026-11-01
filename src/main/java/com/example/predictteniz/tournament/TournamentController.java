package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.PredictionService;
import com.example.predictteniz.prediction.PredictionModels;
import com.example.predictteniz.prediction.MatchConditions;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    private final TournamentService tournaments;
    private final PredictionService predictions;
    private final PredictionModels models;

    public TournamentController(TournamentService tournaments, PredictionService predictions, PredictionModels models) {
        this.tournaments = tournaments;
        this.predictions = predictions;
        this.models = models;
    }

    @GetMapping("/models")
    public List<Map<String, Object>> models() { return models.descriptions(); }

    @GetMapping("/players")
    public PredictionService.Catalog players(@RequestParam(required = false) LocalDate asOf) {
        return predictions.catalog(asOf);
    }

    @GetMapping
    public List<TournamentService.DrawSummary> recent() { return tournaments.recent(); }

    @GetMapping("/{id}")
    public TournamentService.DrawResponse get(@PathVariable UUID id) { return tournaments.get(id); }

    @PostMapping("/preview")
    public TournamentService.DrawResponse preview(@Valid @RequestBody CreateDraw request) {
        return tournaments.preview(request.title(), request.asOf(), request.playerIds(), request.surface(), request.bestOf(), request.modelVersion());
    }

    @PostMapping
    public ResponseEntity<TournamentService.DrawResponse> create(@Valid @RequestBody CreateDraw request) {
        var result = tournaments.create(request.title(), request.asOf(), request.playerIds(), request.surface(), request.bestOf(), request.modelVersion());
        return ResponseEntity.created(URI.create("/api/tournaments/" + result.id())).body(result);
    }

    public record CreateDraw(@NotBlank @Size(max = 100) String title, @NotNull @PastOrPresent LocalDate asOf,
                             @NotNull @Size(min = 2, max = 16) List<@NotNull @Positive Long> playerIds,
                             MatchConditions.Surface surface, Integer bestOf, @Size(max = 64) String modelVersion) {}
}
