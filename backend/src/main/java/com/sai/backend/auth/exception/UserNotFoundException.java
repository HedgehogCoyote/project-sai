package com.sai.backend.auth.exception;

public class UserNotFoundException extends RuntimeException {
	
	public UserNotFoundException()
	{
		super("없는 유저입니다.");
	}
}
