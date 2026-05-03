CREATE TABLE inventory_items (
    id                UUID PRIMARY KEY,
    product_id        VARCHAR(255)   NOT NULL UNIQUE,
    product_name      VARCHAR(255)   NOT NULL,
    quantity          INT            NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT            NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    unit_price        DECIMAL(10, 2) NOT NULL CHECK (unit_price > 0),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_product_id ON inventory_items (product_id);