package com.example.predictteniz.tournament;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournament_draws")
public class TournamentDraw {
    @Id private UUID id;
    @Column(nullable = false, length = 100) private String title;
    @Column(name = "as_of", nullable = false) private LocalDate asOf;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "tournament_entries", joinColumns = @JoinColumn(name = "tournament_id"))
    @OrderColumn(name = "slot")
    @Column(name = "player_id", nullable = false)
    private List<Long> playerIds = new ArrayList<>();

    protected TournamentDraw() {}

    public TournamentDraw(String title, LocalDate asOf, List<Long> playerIds) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.asOf = asOf;
        this.playerIds = new ArrayList<>(playerIds);
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public LocalDate getAsOf() { return asOf; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Long> getPlayerIds() { return List.copyOf(playerIds); }
}
