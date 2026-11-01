package com.example.predictteniz.player.importing;

import com.example.predictteniz.player.Player;
import com.example.predictteniz.player.PlayerExternalId;
import com.example.predictteniz.player.PlayerExternalIdRepository;
import com.example.predictteniz.player.PlayerRanking;
import com.example.predictteniz.player.PlayerRankingRepository;
import com.example.predictteniz.player.RankingTour;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingImportService {
    private static final String SOURCE = "SACKMANN_ATP";
    private final PlayerExternalIdRepository identities;
    private final PlayerRankingRepository rankings;

    public RankingImportService(PlayerExternalIdRepository identities, PlayerRankingRepository rankings) {
        this.identities = identities;
        this.rankings = rankings;
    }

    @Transactional
    public Summary importLatest(Reader input) throws IOException {
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get();
        try (var parser = format.parse(input)) {
            if (!parser.getHeaderMap().keySet().containsAll(Set.of("ranking_date", "rank", "player", "points")))
                throw new IllegalArgumentException("Missing ranking CSV headers.");
            var records = parser.getRecords();
            LocalDate date = records.stream()
                .map(row -> LocalDate.parse(row.get("ranking_date"), DateTimeFormatter.BASIC_ISO_DATE))
                .max(LocalDate::compareTo).orElseThrow(() -> new IllegalArgumentException("Ranking CSV is empty."));
            Map<String, PlayerExternalId> known = identities.findAllBySource(SOURCE).stream()
                .collect(Collectors.toMap(PlayerExternalId::getExternalId, Function.identity()));
            var accepted = new ArrayList<RankingRow>();
            Set<String> seen = new HashSet<>();
            int unknown = 0;
            for (var row : records) {
                if (!row.get("ranking_date").equals(date.format(DateTimeFormatter.BASIC_ISO_DATE))) continue;
                if (!row.isConsistent() || !seen.add(row.get("player")))
                    throw new IllegalArgumentException("Inconsistent or duplicate ranking row.");
                var identity = known.get(row.get("player"));
                if (identity == null) { unknown++; continue; }
                int position = Integer.parseInt(row.get("rank"));
                Integer points = row.get("points").isBlank() ? null : Integer.valueOf(row.get("points"));
                if (position < 1 || (points != null && points < 0))
                    throw new IllegalArgumentException("Ranking position or points are invalid.");
                accepted.add(new RankingRow(identity.getPlayer(), position, points));
            }
            int created = 0;
            Instant retrievedAt = Instant.now();
            for (var row : accepted) {
                var existing = rankings.findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
                    row.player().getId(), RankingTour.ATP, date);
                if (existing.isPresent() && existing.get().getRankingDate().equals(date)) continue;
                rankings.save(new PlayerRanking(row.player(), RankingTour.ATP, date,
                    row.position(), row.points(), SOURCE, retrievedAt));
                created++;
            }
            return new Summary(date, created, accepted.size() - created, unknown);
        }
    }

    private record RankingRow(Player player, int position, Integer points) {}
    public record Summary(LocalDate date, int created, int alreadyKnown, int unknownPlayers) {}
}
