-- A retry by the same account returns the same reservation, not another booking.
ALTER TABLE bookings ADD COLUMN request_key UUID;
CREATE UNIQUE INDEX bookings_user_request_key ON bookings(user_id, request_key);
