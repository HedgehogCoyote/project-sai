package com.sai.backend.auth.exception;

public class InvalidLoginException extends RuntimeException{
	
	public InvalidLoginException()
	{
		super("아이디나 비밀번호가 다릅니다.");
	}

}
