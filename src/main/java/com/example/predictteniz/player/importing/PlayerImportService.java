package com.example.predictteniz.player.importing;

import lombok.RequiredArgsConstructor;

import com.example.predictteniz.player.Player;
import com.example.predictteniz.player.PlayerExternalId;
import com.example.predictteniz.player.PlayerExternalIdRepository;
import com.example.predictteniz.player.PlayerRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PlayerImportService {
    private final PlayerRepository players;
    private final PlayerExternalIdRepository externalIds;

    @Transactional
    public ImportSummary importPlayers(List<PlayerCsvReader.PlayerRow> rows, String source) {
        if (source == null || source.isBlank() || source.strip().length() > 50)
            throw new IllegalArgumentException("Source must have 1-50 characters.");
        String normalizedSource = source.strip().toUpperCase(Locale.ROOT);
        int created = 0;
        int alreadyKnown = 0;
        for (var row : rows) {
            if (externalIds.findBySourceAndExternalId(normalizedSource, row.externalId()).isPresent()) {
                alreadyKnown++;
                continue;
            }
            Player player = players.save(new Player(row.firstName(), row.lastName(), row.countryCode(), row.birthDate()));
            externalIds.save(new PlayerExternalId(player, normalizedSource, row.externalId()));
            created++;
        }
        return new ImportSummary(created, alreadyKnown);
    }

    public record ImportSummary(int created, int alreadyKnown) {}
}
