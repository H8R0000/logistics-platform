--liquibase formatted sql

--changeset logistics:001-create-processing-tasks-and-history
CREATE TABLE processing_tasks (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL UNIQUE,
    owner_id VARCHAR(255) NOT NULL,
    shipment_data JSONB NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN (
        'QUEUED', 'PROCESSING', 'WAITING_FOR_COURIER', 'IN_TRANSIT',
        'COMPLETED', 'FAILED', 'COURIER_NOT_FOUND'
    )),
    current_stage VARCHAR(32) NOT NULL CHECK (current_stage IN (
        'SEARCH_COURIER', 'CREATE_ROUTE', 'CONFIRM_DELIVERY'
    )),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    next_execution_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_processing_tasks_owner_created ON processing_tasks (owner_id, created_at DESC, id);
CREATE INDEX idx_processing_tasks_created ON processing_tasks (created_at DESC, id);
CREATE INDEX idx_processing_tasks_status ON processing_tasks (status);
CREATE INDEX idx_processing_tasks_stage ON processing_tasks (current_stage);

CREATE TABLE processing_task_events (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES processing_tasks (id),
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_stage VARCHAR(32) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processing_task_events_history ON processing_task_events (task_id, created_at, id);

--changeset logistics:002-create-processed-commands
CREATE TABLE processed_commands (
    message_id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

--changeset logistics:003-drop-processed-commands
DROP TABLE processed_commands;

--changeset logistics:004-create-courier-assignments
CREATE TABLE courier_assignments (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES processing_tasks (shipment_id),
    courier_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'RELEASED'))
);

CREATE UNIQUE INDEX idx_courier_assignments_active_courier
    ON courier_assignments (courier_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX idx_courier_assignments_active_shipment
    ON courier_assignments (shipment_id) WHERE status = 'ACTIVE';

CREATE INDEX idx_processing_tasks_due_search ON processing_tasks (next_execution_at, id)
    WHERE current_stage = 'SEARCH_COURIER' AND status IN ('QUEUED', 'WAITING_FOR_COURIER', 'PROCESSING');

--changeset logistics:005-create-route-sheets
CREATE TABLE route_sheets (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL UNIQUE REFERENCES processing_tasks (shipment_id),
    courier_id UUID NOT NULL,
    origin JSONB NOT NULL,
    destination JSONB NOT NULL,
    estimated_delivery_hours NUMERIC(15, 9) NOT NULL CHECK (estimated_delivery_hours > 0),
    estimated_delivery_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('IN_TRANSIT', 'DELIVERED'))
);

CREATE INDEX idx_processing_tasks_due_delivery ON processing_tasks (next_execution_at, id)
    WHERE (current_stage = 'CREATE_ROUTE' AND status = 'PROCESSING')
       OR (current_stage = 'CONFIRM_DELIVERY' AND status = 'IN_TRANSIT');

--changeset logistics:006-create-event-outbox
ALTER TABLE processing_tasks
    ADD COLUMN event_sequence BIGINT NOT NULL DEFAULT 0 CHECK (event_sequence >= 0);

CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES processing_tasks (shipment_id),
    sequence_number BIGINT NOT NULL CHECK (sequence_number > 0),
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    UNIQUE (shipment_id, sequence_number)
);

CREATE INDEX idx_outbox_messages_pending ON outbox_messages (shipment_id, sequence_number)
    WHERE published_at IS NULL;

--changeset logistics:007-route-outbox-to-topics
ALTER TABLE outbox_messages
    ADD COLUMN topic VARCHAR(64) NOT NULL DEFAULT 'shipment-events'
        CHECK (topic IN ('shipment-events', 'shipment-errors'));
ALTER TABLE outbox_messages
    DROP CONSTRAINT outbox_messages_shipment_id_sequence_number_key,
    ADD CONSTRAINT uq_outbox_messages_topic_sequence UNIQUE (topic, shipment_id, sequence_number);
DROP INDEX idx_outbox_messages_pending;
CREATE INDEX idx_outbox_messages_pending ON outbox_messages (topic, shipment_id, sequence_number)
    WHERE published_at IS NULL;
