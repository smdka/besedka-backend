-- liquibase formatted sql

-- changeset besedka:1
CREATE TABLE cabins (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    location          VARCHAR(200) NOT NULL,
    price_per_hour    NUMERIC(10, 2) NOT NULL,
    description       TEXT,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cabin_photos (
    cabin_id  BIGINT NOT NULL REFERENCES cabins (id) ON DELETE CASCADE,
    photo_url TEXT   NOT NULL,
    position  INT    NOT NULL,
    PRIMARY KEY (cabin_id, position)
);

CREATE TABLE clients (
    id               BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT       NOT NULL UNIQUE,
    first_name       VARCHAR(100),
    last_name        VARCHAR(100),
    username         VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
    id                       BIGSERIAL PRIMARY KEY,
    cabin_id                 BIGINT      NOT NULL REFERENCES cabins (id),
    client_id                BIGINT      NOT NULL REFERENCES clients (id),
    date                     DATE        NOT NULL,
    check_in_time            TIME        NOT NULL,
    check_out_time           TIME        NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reminder_before_minutes  INT,
    reminder_sent            BOOLEAN     NOT NULL DEFAULT FALSE,
    admin_message_id         INT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_cabin_date ON bookings (cabin_id, date);
CREATE INDEX idx_bookings_client     ON bookings (client_id);
CREATE INDEX idx_bookings_status     ON bookings (status);
