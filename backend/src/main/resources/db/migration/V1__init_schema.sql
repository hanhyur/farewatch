-- FareWatch 초기 스키마. 5개 테이블.

CREATE TABLE route (
    id              BIGSERIAL PRIMARY KEY,
    origin_iata     VARCHAR(3) NOT NULL,
    destination_iata VARCHAR(3) NOT NULL,
    airline_code    VARCHAR(10),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE fare_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    route_id        BIGINT NOT NULL REFERENCES route(id),
    departure_date  DATE NOT NULL,
    collected_at    TIMESTAMP NOT NULL,
    price           BIGINT NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'KRW',
    source          VARCHAR(50) NOT NULL,
    raw_data        JSONB
);

CREATE INDEX idx_fare_snapshot_route_date
    ON fare_snapshot(route_id, departure_date, collected_at DESC);

CREATE TABLE fare_statistics (
    id              BIGSERIAL PRIMARY KEY,
    route_id        BIGINT NOT NULL REFERENCES route(id),
    departure_date  DATE NOT NULL,
    calculated_at   TIMESTAMP NOT NULL,
    avg_price       BIGINT NOT NULL,
    min_price       BIGINT NOT NULL,
    max_price       BIGINT NOT NULL,
    std_deviation   DOUBLE PRECISION NOT NULL,
    sample_count    INT NOT NULL,
    p25_price       BIGINT,
    p75_price       BIGINT,
    CONSTRAINT uk_fare_statistics_route_departure UNIQUE (route_id, departure_date)
);

CREATE TABLE alert_rule (
    id                      BIGSERIAL PRIMARY KEY,
    route_id                BIGINT NOT NULL REFERENCES route(id),
    user_identifier         VARCHAR(255) NOT NULL,
    departure_date_from     DATE NOT NULL,
    departure_date_to       DATE NOT NULL,
    target_price            BIGINT,
    target_price_currency   VARCHAR(3),
    verdict_trigger         VARCHAR(20) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL
);

CREATE TABLE notification (
    id              BIGSERIAL PRIMARY KEY,
    alert_rule_id   BIGINT NOT NULL REFERENCES alert_rule(id),
    route_id        BIGINT NOT NULL REFERENCES route(id),
    departure_date  DATE NOT NULL,
    sent_at         TIMESTAMP NOT NULL,
    verdict         VARCHAR(20) NOT NULL,
    price_at_send   BIGINT NOT NULL,
    price_at_send_currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    channel         VARCHAR(20) NOT NULL,
    message         TEXT
);

CREATE INDEX idx_notification_dedup
    ON notification(alert_rule_id, departure_date, sent_at DESC);
