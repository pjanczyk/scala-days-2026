CREATE TABLE link (
    code       VARCHAR PRIMARY KEY,
    url        VARCHAR NOT NULL,
    title      VARCHAR,
    visits     BIGINT  NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
