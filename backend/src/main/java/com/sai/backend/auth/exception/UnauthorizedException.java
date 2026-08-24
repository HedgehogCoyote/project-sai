package com.sai.backend.auth.exception;

public class UnauthorizedException extends RuntimeException {
	
	public UnauthorizedException()
	{
		super("Unauthorized");
	}
	
}
