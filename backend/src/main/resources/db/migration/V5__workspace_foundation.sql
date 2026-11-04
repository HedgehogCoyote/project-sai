-- Workspace foundation: space settings, members, shared content, invite links,
-- notices/comments, activity feed, photos, account deletion.

-- 1. Invitation integrity. Older duplicate PENDING rows (same inviter, space, invitee)
--    are closed so the unique index can be created; the earliest invitation stays pending.
UPDATE space_invitation SET status = 'DENIED'
WHERE status = 'PENDING'
  AND id NOT IN (
      SELECT MIN(id) FROM space_invitation WHERE status = 'PENDING'
      GROUP BY space_id, inviter_id, invitee_id);
CREATE UNIQUE INDEX uq_space_invitation_pending
    ON space_invitation (space_id, inviter_id, invitee_id) WHERE status = 'PENDING';

-- 2. Space settings and member state.
ALTER TABLE space ADD COLUMN relation VARCHAR(20) NOT NULL DEFAULT 'FRIENDS'
    CHECK (relation IN ('COUPLE', 'FRIENDS', 'CLUB', 'CUSTOM'));
ALTER TABLE space ADD COLUMN color_primary VARCHAR(7) NOT NULL DEFAULT '#aa849c'
    CHECK (color_primary ~ '^#[0-9a-f]{6}$');
ALTER TABLE space ADD COLUMN color_secondary VARCHAR(7) NOT NULL DEFAULT '#71576c'
    CHECK (color_secondary ~ '^#[0-9a-f]{6}$');
ALTER TABLE space_member ADD COLUMN nickname VARCHAR(30);
ALTER TABLE space_member ADD COLUMN last_seen_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

-- 3. Shared space content (activities and tools), versioned like space_room.
CREATE TABLE space_content (
    space_id BIGINT PRIMARY KEY REFERENCES space(id) ON DELETE CASCADE,
    content JSONB NOT NULL CHECK (jsonb_typeof(content) = 'object'),
    version BIGINT NOT NULL CHECK (version > 0),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 4. Invite links.
CREATE TABLE space_invite_link (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES space(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_space_invite_link_space ON space_invite_link (space_id);

-- 5. Notices and comments.
CREATE TABLE space_post (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES space(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('NOTICE', 'COMMENT')),
    target VARCHAR(120),
    body VARCHAR(2000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_space_post_lookup ON space_post (space_id, kind, target, created_at);

-- 6. Activity feed: recent news, notifications and furniture badges.
CREATE TABLE space_feed (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES space(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES users(id),
    kind VARCHAR(20) NOT NULL,
    module VARCHAR(30),
    summary VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_space_feed_space_created ON space_feed (space_id, created_at DESC);

-- 7. Photos (files live on disk under sai.storage.dir).
CREATE TABLE space_photo (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES space(id) ON DELETE CASCADE,
    uploader_id BIGINT NOT NULL REFERENCES users(id),
    event_id VARCHAR(80),
    caption VARCHAR(200),
    storage_key VARCHAR(160) NOT NULL UNIQUE,
    content_type VARCHAR(40) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_space_photo_space ON space_photo (space_id, created_at DESC);

-- 8. Joining a space: couples join as MANAGER, other pending invitations to the
--    same space are closed, and the join is announced in the feed. A trigger keeps
--    this identical for invitation joins and invite-link joins.
CREATE FUNCTION sai_space_member_joined() RETURNS trigger AS $$
BEGIN
    IF NEW.role = 'MEMBER' AND EXISTS (SELECT 1 FROM space WHERE id = NEW.space_id AND relation = 'COUPLE') THEN
        NEW.role := 'MANAGER';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_space_member_role BEFORE INSERT ON space_member
    FOR EACH ROW EXECUTE FUNCTION sai_space_member_joined();

CREATE FUNCTION sai_space_member_after_join() RETURNS trigger AS $$
BEGIN
    UPDATE space_invitation SET status = 'ACCEPTED'
    WHERE space_id = NEW.space_id AND invitee_id = NEW.user_id AND status = 'PENDING';
    IF EXISTS (SELECT 1 FROM space_member WHERE space_id = NEW.space_id AND id <> NEW.id) THEN
        INSERT INTO space_feed (space_id, actor_id, kind, module, summary, created_at)
        SELECT NEW.space_id, NEW.user_id, 'MEMBER', NULL, u.name || '님이 들어왔습니다.', CURRENT_TIMESTAMP
        FROM users u WHERE u.id = NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_space_member_after_join AFTER INSERT ON space_member
    FOR EACH ROW EXECUTE FUNCTION sai_space_member_after_join();
