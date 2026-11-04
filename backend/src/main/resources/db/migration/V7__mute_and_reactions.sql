-- Per-member mute: a muted space still shows its own badges but stays out of notifications.
ALTER TABLE space_member ADD COLUMN muted BOOLEAN NOT NULL DEFAULT FALSE;

-- One emoji reaction per member per emoji on a notice or comment.
CREATE TABLE space_reaction (
    post_id BIGINT NOT NULL REFERENCES space_post(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    emoji VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (post_id, user_id, emoji)
);
