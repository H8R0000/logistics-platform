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

--changeset logistics:002-create-outbox
CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL UNIQUE REFERENCES shipments (id),
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_messages_pending ON outbox_messages (created_at, id)
    WHERE published_at IS NULL;

--changeset logistics:003-add-processing-event-sequence
ALTER TABLE shipments
    ADD COLUMN last_processing_sequence BIGINT NOT NULL DEFAULT 0 CHECK (last_processing_sequence >= 0);

ALTER TABLE shipment_events
    ADD COLUMN processing_sequence BIGINT NOT NULL DEFAULT 0 CHECK (processing_sequence >= 0);

DROP INDEX idx_shipment_events_history;
CREATE INDEX idx_shipment_events_history
    ON shipment_events (shipment_id, processing_sequence, created_at, id);

--changeset logistics:004-add-external-shipment-identity
ALTER TABLE shipments
    ADD COLUMN source_id VARCHAR(64),
    ADD COLUMN source_message_id UUID,
    ADD CONSTRAINT chk_shipments_external_identity
        CHECK ((source_id IS NULL) = (source_message_id IS NULL)),
    ADD CONSTRAINT uq_shipments_external_identity UNIQUE (source_id, source_message_id);
