-- Trigram-Extension fuer die ILIKE-Suche ueber den GIN-Index
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE json_documents (
    id         uuid  PRIMARY KEY,
    data       jsonb NOT NULL,
    name text  GENERATED ALWAYS AS (data ->> 'name') STORED,
    city text  GENERATED ALWAYS AS (data ->> 'city') STORED
);

CREATE INDEX idx_name_trgm
    ON json_documents USING GIN (name gin_trgm_ops);

CREATE INDEX idx_city_trgm
    ON json_documents USING GIN (city gin_trgm_ops);