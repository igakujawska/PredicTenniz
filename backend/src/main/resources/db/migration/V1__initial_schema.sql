CREATE TABLE player (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(3) NOT NULL,
    hard_elo DOUBLE PRECISION NOT NULL,
    clay_elo DOUBLE PRECISION NOT NULL,
    grass_elo DOUBLE PRECISION NOT NULL
);

CREATE TABLE simulation (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    surface VARCHAR(10) NOT NULL,
    created_at VARCHAR(40) NOT NULL,
    result_json TEXT NOT NULL
);
CREATE INDEX simulation_created_at_idx ON simulation(created_at);

-- Illustrative ratings only; these are not fitted or current ATP rankings.
INSERT INTO player VALUES
('sinner', 'Jannik Sinner', 'ITA', 2210, 2050, 2110),
('alcaraz', 'Carlos Alcaraz', 'ESP', 2160, 2250, 2220),
('zverev', 'Alexander Zverev', 'GER', 2060, 2120, 1970),
('djokovic', 'Novak Djokovic', 'SRB', 2130, 2100, 2190),
('medvedev', 'Daniil Medvedev', 'RUS', 2100, 1920, 2010),
('fritz', 'Taylor Fritz', 'USA', 2040, 1900, 2070),
('ruud', 'Casper Ruud', 'NOR', 1950, 2130, 1820),
('hurkacz', 'Hubert Hurkacz', 'POL', 2010, 1870, 2090),
('rublev', 'Andrey Rublev', 'RUS', 2020, 2070, 1950),
('tsitsipas', 'Stefanos Tsitsipas', 'GRE', 1990, 2130, 1910),
('paul', 'Tommy Paul', 'USA', 2010, 1950, 2050),
('dimitrov', 'Grigor Dimitrov', 'BUL', 2040, 1980, 2020),
('shelton', 'Ben Shelton', 'USA', 1990, 1850, 1990),
('rune', 'Holger Rune', 'DEN', 1970, 2050, 1950),
('de-minaur', 'Alex de Minaur', 'AUS', 2030, 1920, 2050),
('musetti', 'Lorenzo Musetti', 'ITA', 1910, 2060, 2030);
