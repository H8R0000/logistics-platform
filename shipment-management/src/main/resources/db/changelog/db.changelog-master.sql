--liquibase formatted sql

--changeset logistics:001-create-shipments-and-history
CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    origin JSONB NOT NULL,
    destination JSONB NOT NULL,
    weight NUMERIC(15, 3) NOT NULL CHECK (weight > 0),
    dimensions JSONB NOT NULL,
    delivery_type VARCHAR(32) NOT NULL CHECK (delivery_type IN ('STANDARD', 'EXPRESS')),
    status VARCHAR(32) NOT NULL CHECK (status IN (
        'CREATED', 'PROCESSING', 'WAITING_FOR_COURIER', 'IN_TRANSIT',
        'DELIVERED', 'FAILED', 'COURIER_NOT_FOUND'
    )),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_shipments_owner_created ON shipments (owner_id, created_at DESC, id);
CREATE INDEX idx_shipments_created ON shipments (created_at DESC, id);
CREATE INDEX idx_shipments_status ON shipments (status);
CREATE INDEX idx_shipments_delivery_type ON shipments (delivery_type);

CREATE TABLE shipment_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments (id),
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shipment_events_history ON shipment_events (shipment_id, created_at, id);
