package com.sai.backend.space.exception;

public class SpacePermissionDeniedException extends RuntimeException{
	
	public SpacePermissionDeniedException() {
		super("해당 Space에 대한 충분한 권한이 없습니다.");
	}

}
