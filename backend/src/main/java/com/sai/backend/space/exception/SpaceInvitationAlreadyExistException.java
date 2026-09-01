package com.sai.backend.space.exception;

public class SpaceInvitationAlreadyExistException extends RuntimeException {
	
	public SpaceInvitationAlreadyExistException() {
		super("SPACE_INVITATION_ALREADY_EXIST");
	}
}
