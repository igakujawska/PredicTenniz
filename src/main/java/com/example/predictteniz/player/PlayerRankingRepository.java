package com.example.predictteniz.player;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRankingRepository extends JpaRepository<PlayerRanking, Long> {

    @Query("""
        select r from PlayerRanking r join fetch r.player
        where r.tour = :tour and r.rankingDate = (
            select max(previous.rankingDate) from PlayerRanking previous
            where previous.player.id = r.player.id and previous.tour = :tour and previous.rankingDate <= :asOf)
        """)
    List<PlayerRanking> findLatestAsOf(@Param("tour") RankingTour tour, @Param("asOf") LocalDate asOf);

    Optional<PlayerRanking> findFirstByPlayer_IdAndTourAndRankingDateLessThanEqualOrderByRankingDateDesc(
        Long playerId, RankingTour tour, LocalDate asOf);

    @Query("""
        select min(r.position) from PlayerRanking r
        where r.player.id = :playerId and r.tour = :tour and r.rankingDate <= :asOf
        """)
    Optional<Integer> findBestRecordedPositionAsOf(@Param("playerId") Long playerId,
        @Param("tour") RankingTour tour, @Param("asOf") LocalDate asOf);
}
