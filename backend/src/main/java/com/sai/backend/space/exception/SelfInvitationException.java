package com.sai.backend.space.exception;

public class SelfInvitationException extends RuntimeException {

	public SelfInvitationException()
	{
		super("SELF_INVITATION");
	}
}
