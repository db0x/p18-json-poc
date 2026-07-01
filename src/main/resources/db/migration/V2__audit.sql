-- Trigram-Extension fuer die ILIKE-Suche ueber den GIN-Index
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE json_documents_audit (
    id          uuid PRIMARY KEY,
    document_id uuid NOT NULL,
    data_old    jsonb,
    data_new    jsonb,
    audited_at  TIMESTAMPTZ NOT NULL,
    action_type varchar(32) NOT NULL,
    user_id     uuid NOT NULL
);

