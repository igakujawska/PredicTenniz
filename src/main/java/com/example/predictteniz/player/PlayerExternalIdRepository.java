package com.example.predictteniz.player;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerExternalIdRepository extends JpaRepository<PlayerExternalId, Long> {
    List<PlayerExternalId> findAllBySource(String source);
    Optional<PlayerExternalId> findBySourceAndExternalId(String source, String externalId);
}
