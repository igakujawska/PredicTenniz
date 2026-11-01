package com.example.predictteniz.player;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerExternalIdRepository extends JpaRepository<PlayerExternalId, Long> {
    Optional<PlayerExternalId> findBySourceAndExternalId(String source, String externalId);
}
