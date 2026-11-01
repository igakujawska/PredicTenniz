ALTER TABLE tournament_draws
    ADD COLUMN surface VARCHAR(5) NOT NULL DEFAULT 'HARD',
    ADD COLUMN best_of SMALLINT NOT NULL DEFAULT 3,
    ADD COLUMN model_version VARCHAR(64) NOT NULL DEFAULT 'ranking-baseline-v1',
    ADD CONSTRAINT tournament_surface_valid CHECK (surface IN ('HARD', 'CLAY', 'GRASS')),
    ADD CONSTRAINT tournament_best_of_valid CHECK (best_of IN (3, 5)),
    ADD CONSTRAINT tournament_model_not_blank CHECK (length(trim(model_version)) > 0);
