package com.sai.backend.global.exception;

public record ApiErrorResponse(
		String code,
		String message) {

}
