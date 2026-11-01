-- ATP and WTA singles ranking observations.
CREATE TABLE player_rankings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    player_id BIGINT NOT NULL,
    tour VARCHAR(3) NOT NULL,
    ranking_date DATE NOT NULL,
    position INTEGER NOT NULL,
    points INTEGER,
    source VARCHAR(100) NOT NULL,
    retrieved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT player_rankings_player_fk
        FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT player_rankings_player_tour_date_unique
        UNIQUE (player_id, tour, ranking_date),
    CONSTRAINT player_rankings_tour_valid
        CHECK (tour IN ('ATP', 'WTA')),
    CONSTRAINT player_rankings_position_positive
        CHECK (position > 0),
    CONSTRAINT player_rankings_points_non_negative
        CHECK (points >= 0),
    CONSTRAINT player_rankings_source_not_blank
        CHECK (length(trim(source)) > 0)
);
