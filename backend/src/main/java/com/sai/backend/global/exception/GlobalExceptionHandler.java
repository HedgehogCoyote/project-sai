package com.sai.backend.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sai.backend.auth.exception.DuplicateLoginIdException;
import com.sai.backend.auth.exception.InvalidLoginException;
import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.auth.exception.UserNotFoundException;

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
	
	
}
