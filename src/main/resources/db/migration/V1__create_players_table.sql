CREATE TABLE players (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    country_code VARCHAR(3),
    birth_date DATE,

    CONSTRAINT players_first_name_not_blank
        CHECK (length(trim(first_name)) > 0),
    CONSTRAINT players_last_name_not_blank
        CHECK (length(trim(last_name)) > 0),
    CONSTRAINT players_country_code_format
        CHECK (country_code ~ '^[A-Z]{3}$')
);
