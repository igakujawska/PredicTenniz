package com.example.predictteniz.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_external_ids")
public class PlayerExternalId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    protected PlayerExternalId() {}

    public PlayerExternalId(Player player, String source, String externalId) {
        this.player = player;
        this.source = source;
        this.externalId = externalId;
    }

    public Player getPlayer() { return player; }
    public String getSource() { return source; }
    public String getExternalId() { return externalId; }
}
