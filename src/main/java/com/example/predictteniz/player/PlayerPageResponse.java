package com.example.predictteniz.player;

import java.util.List;

public record PlayerPageResponse(List<PlayerResponse> items, int page, int size,
                                 long totalElements, int totalPages) {
}
