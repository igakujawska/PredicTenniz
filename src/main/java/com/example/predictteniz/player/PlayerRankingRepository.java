package com.example.predictteniz.player;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRankingRepository extends JpaRepository<PlayerRanking, Long> {

    Optional<PlayerRanking> findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
        Long playerId, RankingTour tour, LocalDate asOf);

    @Query("""
        select min(r.position) from PlayerRanking r
        where r.player.id = :playerId and r.tour = :tour and r.rankingDate <= :asOf
        """)
    Optional<Integer> findBestRecordedPositionAsOf(@Param("playerId") Long playerId,
        @Param("tour") RankingTour tour, @Param("asOf") LocalDate asOf);
}
