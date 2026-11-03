ALTER TABLE tournament_draws ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
-- Slot swaps can temporarily repeat a player while Hibernate updates ordered rows.
ALTER TABLE tournament_entries
    DROP CONSTRAINT tournament_entries_tournament_id_player_id_key;
ALTER TABLE tournament_entries ADD CONSTRAINT tournament_entries_unique_player
    UNIQUE (tournament_id, player_id) DEFERRABLE INITIALLY DEFERRED;
