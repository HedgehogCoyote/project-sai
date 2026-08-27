package com.sai.backend.space.exception;

public class SpaceNotFoundException extends RuntimeException {
	
	public SpaceNotFoundException()
	{
		super("해당 Space를 찾을 수 없습니다.");
	}
}
