package com.sai.backend.space.exception;

public class SpaceMemberAlreadyExist extends RuntimeException {

	public SpaceMemberAlreadyExist()
	{
		super("이미 존재하는 멤버입니다.");
	}
}
