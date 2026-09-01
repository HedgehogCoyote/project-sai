package com.sai.backend.space.exception;

import javax.management.RuntimeErrorException;

public class SpaceInvitationNotFoundException extends RuntimeException {

	public SpaceInvitationNotFoundException() {
		super("Such Space Invitation Not Found");
	}
	
}
