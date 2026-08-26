CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255)
);


CREATE TABLE space (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);


CREATE TABLE space_member (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL,
	
    CONSTRAINT fk_space_member_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_space_member_space
        FOREIGN KEY (space_id)
        REFERENCES space(id),

    CONSTRAINT uq_space_member_user_space
        UNIQUE (user_id, space_id)
);


CREATE TABLE space_invitation (
    id BIGSERIAL PRIMARY KEY,
    space_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_space_invitation_space
        FOREIGN KEY (space_id)
        REFERENCES space(id),

    CONSTRAINT fk_space_invitation_inviter
        FOREIGN KEY (inviter_id)
        REFERENCES users(id),

    CONSTRAINT fk_space_invitation_invitee
        FOREIGN KEY (invitee_id)
        REFERENCES users(id)
);