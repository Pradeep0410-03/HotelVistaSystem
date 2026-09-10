CREATE TABLE admin_cancellations (
    booking_id BIGINT PRIMARY KEY REFERENCES bookings(id),
    actor_email VARCHAR(254) NOT NULL,
    reason VARCHAR(500) NOT NULL CHECK (btrim(reason)<>''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
