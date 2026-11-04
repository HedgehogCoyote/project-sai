package com.sai.backend.workspace;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.sai.backend.global.exception.ApiErrorResponse;

@RestControllerAdvice
public class WorkspaceExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> api(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ApiErrorResponse(e.code(), e.getMessage()));
    }

    /** Two requests raced on the same row (double accept, duplicate invite): answer 409, not 500. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> integrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "ALREADY_PROCESSED", "이미 처리된 요청입니다. 새로고침한 뒤 다시 확인해 주세요."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(new ApiErrorResponse(
                "FILE_TOO_LARGE", "사진은 한 장에 10MB까지 올릴 수 있습니다."));
    }
}
