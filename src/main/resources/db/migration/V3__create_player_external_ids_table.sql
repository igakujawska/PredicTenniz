CREATE TABLE player_external_ids (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id),
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(100) NOT NULL,

    CONSTRAINT player_external_ids_source_id_unique UNIQUE (source, external_id),
    CONSTRAINT player_external_ids_player_source_unique UNIQUE (player_id, source),
    CONSTRAINT player_external_ids_source_not_blank CHECK (length(trim(source)) > 0),
    CONSTRAINT player_external_ids_id_not_blank CHECK (length(trim(external_id)) > 0)
);
