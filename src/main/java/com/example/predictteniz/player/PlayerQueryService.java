package com.example.predictteniz.player;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PlayerQueryService {

    private final PlayerRepository players;

    public PlayerPageResponse list(int page, int size) {
        var result = players.findAll(PageRequest.of(page, size, Sort.by("lastName", "firstName", "id")));
        return new PlayerPageResponse(result.getContent().stream().map(PlayerResponse::from).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
