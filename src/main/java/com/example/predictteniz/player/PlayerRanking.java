package com.example.predictteniz.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "player_rankings")
    public class PlayerRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private RankingTour tour;

    @Column(name = "ranking_date", nullable = false)
    private LocalDate rankingDate;

    @Column(nullable = false)
    private int position;

    private Integer points;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    public PlayerRanking(Player player, RankingTour tour, LocalDate rankingDate,
                         int position, Integer points, String source, Instant retrievedAt) {
        this.player = player;
        this.tour = tour;
        this.rankingDate = rankingDate;
        this.position = position;
        this.points = points;
        this.source = source;
        this.retrievedAt = retrievedAt;
    }

}
