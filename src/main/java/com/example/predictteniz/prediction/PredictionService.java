package com.example.predictteniz.prediction;

import com.example.predictteniz.player.Player;
import com.example.predictteniz.player.PlayerExternalIdRepository;
import com.example.predictteniz.player.PlayerRanking;
import com.example.predictteniz.player.PlayerRankingRepository;
import com.example.predictteniz.player.PlayerRepository;
import com.example.predictteniz.player.RankingTour;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PredictionService {
    private final PlayerRepository players;
    private final PlayerRankingRepository rankings;
    private final PlayerExternalIdRepository identities;

    public List<RankedPlayer> snapshots(List<Long> ids, LocalDate asOf) {
        var profiles = players.findAllById(ids).stream().collect(Collectors.toMap(Player::getId, Function.identity()));
        var history = rankings.findLatestAsOf(RankingTour.ATP, asOf).stream()
            .collect(Collectors.toMap(ranking -> ranking.getPlayer().getId(), Function.identity()));
        var sourceIds = sourceIds();
        return ids.stream().map(id -> {
            var player = profiles.get(id);
            if (player == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono zawodnika: " + id);
            var ranking = history.get(id);
            if (ranking == null) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Brak rankingu ATP dla " + player.getLastName() + " do " + asOf);
            return snapshot(player, ranking, sourceIds.get(id));
        }).toList();
    }

    public Catalog catalog(LocalDate asOf) {
        LocalDate cutoff = asOf == null ? LocalDate.now() : asOf;
        var sourceIds = sourceIds();
        var result = rankings.findLatestAsOf(RankingTour.ATP, cutoff).stream()
            .map(ranking -> snapshot(ranking.getPlayer(), ranking, sourceIds.get(ranking.getPlayer().getId())))
            .sorted(Comparator.comparingInt(RankedPlayer::position).thenComparingLong(RankedPlayer::id)).toList();
        LocalDate latest = result.stream().map(RankedPlayer::rankingDate).max(LocalDate::compareTo).orElse(null);
        return new Catalog(result, latest, "ranking-baseline-v1");
    }

    private Map<Long, String> sourceIds() {
        return identities.findAllBySource("SACKMANN_ATP").stream()
            .collect(Collectors.toMap(identity -> identity.getPlayer().getId(), identity -> identity.getExternalId()));
    }

    private RankedPlayer snapshot(Player player, PlayerRanking ranking, String sourceId) {
        return RankedPlayer.builder().id(player.getId()).name(player.getFirstName() + " " + player.getLastName())
            .countryCode(player.getCountryCode()).position(ranking.getPosition()).rankingDate(ranking.getRankingDate())
            .sourceId(sourceId).build();
    }

    @Builder
    public record RankedPlayer(long id, String name, String countryCode, int position, LocalDate rankingDate, String sourceId) {
        public RankedPlayer(long id, String name, String countryCode, int position, LocalDate rankingDate) {
            this(id, name, countryCode, position, rankingDate, null);
        }
    }
    public record Catalog(List<RankedPlayer> players, LocalDate latestRankingDate, String modelVersion) {}
}
