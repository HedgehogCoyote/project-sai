package com.sai.backend.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sai.backend.auth.exception.DuplicateLoginIdException;
import com.sai.backend.auth.exception.InvalidLoginException;
import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.space.exception.InvalidInvitationStatusException;
import com.sai.backend.space.exception.InvitationPermissionDenied;
import com.sai.backend.space.exception.SpaceInvitationAlreadyExistException;
import com.sai.backend.space.exception.SpaceInvitationNotFoundException;
import com.sai.backend.space.exception.SpaceMemberAlreadyExist;
import com.sai.backend.space.exception.SpaceNotFoundException;
import com.sai.backend.space.exception.SpacePermissionDeniedException;

//여러 Controller에서 발생한 예외를 공통으로 처리
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	// DuplicateLogin Id Exception이 나면 
	@ExceptionHandler(DuplicateLoginIdException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateLoginId
	(DuplicateLoginIdException e)
	{
		
		// API 에러 났다는 리스폰 DTO 생성 
		ApiErrorResponse errorResponse = 
				new ApiErrorResponse(
						"DUPLICATE_LOGIN_ID",
						e.getMessage()
						);
		
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(errorResponse);
		
	}
	
	// InValid Login이 되면,
	@ExceptionHandler(InvalidLoginException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidLogin(
			InvalidLoginException e
			)
	{
		
		ApiErrorResponse loginErrorResponse = 
				new ApiErrorResponse(
						"INVALID_LOGIN",
						e.getMessage()
						);
		
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(loginErrorResponse);
		
	}
	
	// USER 찾을 수 없음
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleUserNotFound(
			UserNotFoundException e
			)
	{
		
		ApiErrorResponse userNotFoundErrorResponse =
				new ApiErrorResponse(
						"USER_NOT_FOUND_EXCEPTION",
						e.getMessage()
						);
		
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(userNotFoundErrorResponse);
		
	}
	
	// 비인가접근
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException e)
	{
		ApiErrorResponse unauthorizedErrorResponse =
				new ApiErrorResponse(
						"AUTHORIZATION_NEEDED",
						e.getMessage()
						);
		
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(unauthorizedErrorResponse);
		
		
	}

	@ExceptionHandler(SpaceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleSpaceNotFound(SpaceNotFoundException e)
	{
		ApiErrorResponse spaceNotFoundResponse
				= new ApiErrorResponse(
				"SPACE_NOT_FOUND",
				e.getMessage());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(spaceNotFoundResponse);
	}

	@ExceptionHandler(SpacePermissionDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleSpacePermissionDenied(SpacePermissionDeniedException e)
	{
		ApiErrorResponse spacePermissionDeniedResponse
				= new ApiErrorResponse(
				"SPACE_PERMISSION_DENIED",
				e.getMessage());

		return ResponseEntity
				.status(HttpStatus.FORBIDDEN)
				.body(spacePermissionDeniedResponse);
	}

	@ExceptionHandler(SpaceMemberAlreadyExist.class)
	public ResponseEntity<ApiErrorResponse> handleSpaceMemberAlreadyExist(SpaceMemberAlreadyExist e)
	{
		ApiErrorResponse spaceMemberAlreadyExistResponse
				= new ApiErrorResponse(
						"SPACE_MEMBER_ALREADY_EXISTS",
				e.getMessage());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(spaceMemberAlreadyExistResponse);
	}

	@ExceptionHandler(SpaceInvitationAlreadyExistException.class)
	public ResponseEntity<ApiErrorResponse> handleSpaceInvitationAlreadyExistException(SpaceInvitationAlreadyExistException e)
	{
		ApiErrorResponse spaceInvitationAlreadyExistException
				= new ApiErrorResponse(
						"SPACE_INVITATION_ALREADY_EXIST",
				e.getMessage());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(spaceInvitationAlreadyExistException);
	}

	
	@ExceptionHandler(SpaceInvitationNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleSpaceInvitationNotFound(SpaceInvitationNotFoundException e)
	{
		ApiErrorResponse spaceInvitationNotFound
				= new ApiErrorResponse(
						"SUCH_SPACE_INVITATION_NOT_FOUND",
				e.getMessage());

		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(spaceInvitationNotFound);
	}

	
	@ExceptionHandler(InvalidInvitationStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidInvitationStatusException(InvalidInvitationStatusException e)
	{
		ApiErrorResponse invalidInvitationStatus
		= new ApiErrorResponse(
				"INVALID_INVITATION",
		e.getMessage());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(invalidInvitationStatus);
	}
	
	@ExceptionHandler(InvitationPermissionDenied.class)
	public ResponseEntity<ApiErrorResponse> handleInvitationPermissionDenied(InvitationPermissionDenied e)
	{
		ApiErrorResponse invitationPermissionDenied
		= new ApiErrorResponse(
				"INVITATION_PERMISSION_DENIED",
		e.getMessage());

		return ResponseEntity
				.status(HttpStatus.FORBIDDEN)
				.body(invitationPermissionDenied);
	}
	
}
