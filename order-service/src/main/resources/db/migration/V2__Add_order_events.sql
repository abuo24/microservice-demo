CREATE TABLE order_events (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id     UUID        NOT NULL,
    event_type       VARCHAR(50) NOT NULL,
    payload          JSONB       NOT NULL,
    sequence_number  BIGSERIAL   NOT NULL,
    occurred_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published        BOOLEAN     NOT NULL DEFAULT FALSE,
    published_at     TIMESTAMP,

    CONSTRAINT pk_order_events PRIMARY KEY (id)
);

CREATE INDEX idx_order_events_aggregate_id ON order_events(aggregate_id);
CREATE INDEX idx_order_events_published    ON order_events(published) WHERE published = FALSE;
CREATE INDEX idx_order_events_sequence     ON order_events(sequence_number);