CREATE TABLE order_projections (
    id               UUID        NOT NULL PRIMARY KEY,
    aggregate_id     UUID        NOT NULL,
    customer_id      VARCHAR(255) NOT NULL,
    status           VARCHAR(50) NOT NULL,
    total_amount     DECIMAL(19, 2) NOT NULL,
    item_count       INTEGER     NOT NULL DEFAULT 0,
    last_event_sequence BIGINT   NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_projections_aggregate_id FOREIGN KEY (aggregate_id) REFERENCES orders(id)
);
CREATE INDEX idx_order_projections_aggregate_id ON order_projections(aggregate_id);
CREATE INDEX idx_order_projections_customer_id ON order_projections(customer_id);
CREATE INDEX idx_order_projections_status ON order_projections(status);