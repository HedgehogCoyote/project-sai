CREATE TABLE space_invitation
(
	id BIGSERIAL PRIMARY KEY,
	space_id BIGINT NOT NULL ,
	inviter_user_id BIGINT NOT NULL ,
	invitee_user_id BIGINT NOT NULL,
	invited_at TIMESTAMP NOT NULL,
	status VARCHAR(20) NOT NULL,
	
	CONSTRAINT fk_space_invitation_space
		FOREIGN KEY(space_id) 
		REFERENCES space(id),
		
	CONSTRAINT fk_space_invitation_inviter_user
		FOREIGN KEY(inviter_user_id)
		REFERENCES users(id),
	
	CONSTRAINT fk_space_invitation_invitee_user
		FOREIGN KEY(invitee_user_id)
		REFERENCES users(id)
	-- CONSTRAINT uq_space_inviter_inviter
	--	UNIQUE(space_id, inviter_user_id, invitee_user_id)
)
