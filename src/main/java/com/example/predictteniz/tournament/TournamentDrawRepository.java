package com.example.predictteniz.tournament;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentDrawRepository extends JpaRepository<TournamentDraw, UUID> {
    List<TournamentDraw> findTop20ByOrderByCreatedAtDesc();
}
