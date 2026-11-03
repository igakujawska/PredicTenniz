package com.example.predictteniz.prediction;

import lombok.RequiredArgsConstructor;

import com.example.predictteniz.player.Player;
import com.example.predictteniz.player.PlayerRanking;
import com.example.predictteniz.player.PlayerRankingRepository;
import com.example.predictteniz.player.PlayerRepository;
import com.example.predictteniz.player.RankingTour;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final WinProbabilityModel model;

    public List<RankedPlayer> snapshots(List<Long> ids, LocalDate asOf) {
        return ids.stream().map(id -> {
            Player player = players.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleziono zawodnika: " + id));
            return snapshot(player, asOf).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Brak rankingu ATP dla " + player.getLastName() + " do " + asOf));
        }).toList();
    }

    public Catalog catalog(LocalDate asOf) {
        LocalDate cutoff = asOf == null ? LocalDate.now() : asOf;
        var result = players.findAll().stream().map(player -> snapshot(player, cutoff))
            .flatMap(Optional::stream).sorted(Comparator.comparingInt(RankedPlayer::position)
                .thenComparingLong(RankedPlayer::id)).toList();
        LocalDate latest = result.stream().map(RankedPlayer::rankingDate).max(LocalDate::compareTo).orElse(null);
        return new Catalog(result, latest, model.version());
    }

    private Optional<RankedPlayer> snapshot(Player player, LocalDate asOf) {
        return rankings.findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
            player.getId(), RankingTour.ATP, asOf).map(ranking -> toSnapshot(player, ranking));
    }

    private RankedPlayer toSnapshot(Player player, PlayerRanking ranking) {
        return new RankedPlayer(player.getId(), player.getFirstName() + " " + player.getLastName(),
            player.getCountryCode(), ranking.getPosition(), ranking.getRankingDate());
    }

    public record RankedPlayer(long id, String name, String countryCode, int position, LocalDate rankingDate) {}
    public record Catalog(List<RankedPlayer> players, LocalDate latestRankingDate, String modelVersion) {}
}
