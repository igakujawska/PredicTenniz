package com.example.predictteniz.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "players")
    public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public Player(String firstName, String lastName, String countryCode, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.countryCode = countryCode;
        this.birthDate = birthDate;
    }

}
