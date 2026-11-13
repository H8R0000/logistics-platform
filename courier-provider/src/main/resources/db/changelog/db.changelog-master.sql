--liquibase formatted sql

--changeset logistics:001-create-couriers
CREATE TABLE couriers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(32) NOT NULL CHECK (vehicle_type IN ('BIKE', 'CAR', 'VAN')),
    max_weight NUMERIC(15, 3) NOT NULL CHECK (max_weight > 0),
    current_load NUMERIC(15, 3) NOT NULL CHECK (current_load >= 0 AND current_load <= max_weight),
    available BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

--changeset logistics:002-add-demo-couriers
INSERT INTO couriers (id, name, vehicle_type, max_weight, current_load, available, updated_at)
VALUES
    ('11111111-1111-4111-8111-111111111111', 'Иван Петров', 'CAR', 50, 10, TRUE, CURRENT_TIMESTAMP),
    ('22222222-2222-4222-8222-222222222222', 'Анна Смирнова', 'BIKE', 5, 5, FALSE, CURRENT_TIMESTAMP),
    ('33333333-3333-4333-8333-333333333333', 'Павел Иванов', 'VAN', 1000, 0, TRUE, CURRENT_TIMESTAMP);
