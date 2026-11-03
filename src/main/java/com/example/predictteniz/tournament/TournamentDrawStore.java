package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.MatchConditions;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentDrawStore {
    private final TournamentDrawRepository draws;

    public StoredDraw get(UUID id) { return snapshot(find(id)); }

    public List<TournamentService.DrawSummary> recent() {
        return draws.findTop20ByOrderByCreatedAtDesc().stream().map(draw ->
            new TournamentService.DrawSummary(draw.getId(), draw.getTitle(), draw.getAsOf(), (int)draw.getPlayerIds().stream().filter(java.util.Objects::nonNull).count())).toList();
    }

    @Transactional
    public StoredDraw create(String title, LocalDate asOf, List<Long> ids, MatchConditions conditions, String modelVersion, TournamentFormat formatCode) {
        return snapshot(draws.saveAndFlush(new TournamentDraw(title, asOf, ids, conditions, modelVersion, formatCode)));
    }

    @Transactional
    public StoredDraw update(UUID id, Long version, String title, LocalDate asOf, List<Long> ids,
                             MatchConditions conditions, String modelVersion, TournamentFormat formatCode) {
        var draw = find(id);
        if (version == null || draw.getVersion() != version)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Turniej zmienił się od odczytu. Otwórz go ponownie albo zapisz kopię.");
        draw.revise(title, asOf, ids, conditions, modelVersion, formatCode);
        return snapshot(draws.saveAndFlush(draw));
    }

    private TournamentDraw find(UUID id) {
        return draws.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono drabinki."));
    }

    private StoredDraw snapshot(TournamentDraw draw) {
        return new StoredDraw(draw.getId(), draw.getTitle(), draw.getAsOf(), draw.getCreatedAt(), draw.getVersion(),
            draw.getPlayerIds(), draw.getConditions(), draw.getModelVersion(), draw.getFormatCode());
    }

    public record StoredDraw(UUID id, String title, LocalDate asOf, Instant createdAt, long version,
                             List<Long> playerIds, MatchConditions conditions, String modelVersion, TournamentFormat formatCode) {}
}
