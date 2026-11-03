ALTER TABLE tournament_draws ADD COLUMN format_code VARCHAR(32) NOT NULL DEFAULT 'CUSTOM';
ALTER TABLE tournament_draws ADD COLUMN slot_count INTEGER;
UPDATE tournament_draws d SET slot_count = (SELECT COALESCE(MAX(e.slot), 1) + 1 FROM tournament_entries e WHERE e.tournament_id=d.id);
ALTER TABLE tournament_draws ALTER COLUMN slot_count SET NOT NULL;
ALTER TABLE tournament_draws ADD CONSTRAINT draw_slot_count CHECK (slot_count IN (2,4,8,16,32,64,128));
ALTER TABLE tournament_entries ALTER COLUMN player_id DROP NOT NULL;
ALTER TABLE tournament_entries DROP CONSTRAINT tournament_entries_slot_check;
ALTER TABLE tournament_entries ADD CONSTRAINT tournament_entries_slot_check CHECK (slot >= 0 AND slot < 128);
