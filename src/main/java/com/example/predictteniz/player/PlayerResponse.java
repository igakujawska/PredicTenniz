package com.example.predictteniz.player;

import java.time.LocalDate;

public record PlayerResponse(Long id, String firstName, String lastName,
                             String countryCode, LocalDate birthDate) {

    static PlayerResponse from(Player player) {
        return new PlayerResponse(player.getId(), player.getFirstName(), player.getLastName(),
            player.getCountryCode(), player.getBirthDate());
    }
}
