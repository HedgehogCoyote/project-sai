CREATE TABLE space_room (
    space_id BIGINT PRIMARY KEY REFERENCES space(id),
    layout JSONB NOT NULL CHECK (jsonb_typeof(layout) = 'object'),
    version BIGINT NOT NULL CHECK (version > 0),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
