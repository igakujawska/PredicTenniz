package dev.courtvision;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final PlayerRepository players;
    private final PredictionService predictions;
    private final DrawService draws;
    private final SimulationRepository simulations;
    private final TennisProvider provider;
    public ApiController(PlayerRepository players, PredictionService predictions, DrawService draws,
                         SimulationRepository simulations, TennisProvider provider) {
        this.players=players; this.predictions=predictions; this.draws=draws; this.simulations=simulations; this.provider=provider;
    }

    @GetMapping("/players") public List<Player> players() { return players.findAll(); }

    @PostMapping("/predictions") public Prediction predict(@Valid @RequestBody PredictionRequest request) {
        if (request.firstId().equals(request.secondId())) throw new IllegalArgumentException("Choose two different players.");
        Player first = players.require(request.firstId()), second = players.require(request.secondId());
        return new Prediction(first, second, request.surface(), predictions.probability(first, second, request.surface()),
            first.rating(request.surface()) - second.rating(request.surface()), PredictionService.MODEL_VERSION, "ILLUSTRATIVE");
    }

    @PostMapping("/simulations") @ResponseStatus(HttpStatus.CREATED)
    public DrawResult simulate(@Valid @RequestBody DrawRequest request) {
        DrawResult result = draws.calculate(request);
        simulations.save(result);
        return result;
    }
    @PostMapping("/draws/preview") public DrawResult preview(@Valid @RequestBody DrawRequest request) {
        return draws.calculate(request);
    }
    @GetMapping("/simulations") public List<SimulationRepository.Summary> recent() { return simulations.recent(); }
    @GetMapping("/simulations/{id}") public DrawResult load(@PathVariable String id) { return simulations.require(id); }
    @GetMapping("/integrations/status") public Map<String,Object> status() {
        return Map.of("provider", "API Tennis", "configured", provider.configured(), "demoData", true);
    }
    @GetMapping("/integrations/fixtures") public List<TennisProvider.Fixture> fixtures(@RequestParam LocalDate date) {
        return provider.fixtures(date);
    }
    public record PredictionRequest(@NotBlank String firstId, @NotBlank String secondId, @NotNull Surface surface) {}
    public record Prediction(Player first, Player second, Surface surface, double firstProbability, double ratingDifference,
                             String modelVersion, String dataMode) {}
}
