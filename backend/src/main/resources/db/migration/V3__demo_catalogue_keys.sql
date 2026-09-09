-- Nullable marker: real/manual properties remain untouched by the demo loader.
ALTER TABLE properties ADD COLUMN demo_key VARCHAR(64) UNIQUE;
