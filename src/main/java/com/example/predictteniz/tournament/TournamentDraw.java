package com.example.predictteniz.tournament;

import com.example.predictteniz.prediction.MatchConditions;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CollectionTable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tournament_draws")
public class TournamentDraw {
    @Id private UUID id;
    @Version private Long version;
    @Column(nullable = false, length = 100) private String title;
    @Column(name = "as_of", nullable = false) private LocalDate asOf;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5) private MatchConditions.Surface surface;
    @Column(name = "best_of", nullable = false) private int bestOf;
    @Column(name = "model_version", nullable = false, length = 64) private String modelVersion;

    @ElementCollection
    @CollectionTable(name = "tournament_entries", joinColumns = @JoinColumn(name = "tournament_id"))
    @OrderColumn(name = "slot")
    @Column(name = "player_id", nullable = false)
    private List<Long> playerIds = new ArrayList<>();

    public TournamentDraw(String title, LocalDate asOf, List<Long> playerIds) {
        this(title, asOf, playerIds, MatchConditions.DEFAULT, "ranking-baseline-v1");
    }

    public TournamentDraw(String title, LocalDate asOf, List<Long> playerIds, MatchConditions conditions, String modelVersion) {
        this.surface = conditions.surface();
        this.bestOf = conditions.bestOf();
        this.modelVersion = modelVersion;
        this.id = UUID.randomUUID();
        this.title = title;
        this.asOf = asOf;
        this.playerIds = new ArrayList<>(playerIds);
        this.createdAt = Instant.now();
    }

    public void revise(String title, LocalDate asOf, List<Long> ids, MatchConditions conditions, String modelVersion) {
        this.title = title;
        this.asOf = asOf;
        this.surface = conditions.surface();
        this.bestOf = conditions.bestOf();
        this.modelVersion = modelVersion;
        this.playerIds.clear();
        this.playerIds.addAll(ids);
    }

    public MatchConditions getConditions() { return new MatchConditions(surface, bestOf); }

    public List<Long> getPlayerIds() { return List.copyOf(playerIds); }
}
