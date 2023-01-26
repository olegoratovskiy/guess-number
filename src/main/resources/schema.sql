CREATE TABLE IF NOT EXISTS Result (
    id       VARCHAR(60)  PRIMARY KEY,
    score    INTEGER      NOT NULL
    );

CREATE TABLE IF NOT EXISTS GuessedNumber (
    id                VARCHAR(60)  PRIMARY KEY,
    secret_number     INTEGER      NOT NULL
);
