package com.sai.backend.auth.exception;

public class DuplicateLoginIdException extends RuntimeException {
	
	public DuplicateLoginIdException()
	{
		super("이미 존재하는 사용자입니다.");
	}

}
