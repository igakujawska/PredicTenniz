CREATE TABLE tournament_draws (
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL CHECK (length(trim(title)) > 0),
    as_of DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tournament_entries (
    tournament_id UUID NOT NULL REFERENCES tournament_draws(id) ON DELETE CASCADE,
    slot INTEGER NOT NULL CHECK (slot >= 0 AND slot < 16),
    player_id BIGINT NOT NULL REFERENCES players(id),
    PRIMARY KEY (tournament_id, slot),
    UNIQUE (tournament_id, player_id)
);
