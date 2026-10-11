package com.sai.backend.room.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sai.backend.global.exception.ApiErrorResponse;
import com.sai.backend.room.exception.InvalidRoomException;
import com.sai.backend.room.exception.RoomConflictException;

@RestControllerAdvice(assignableTypes = RoomController.class)
public class RoomExceptionHandler {
    @ExceptionHandler(InvalidRoomException.class)
    public ResponseEntity<ApiErrorResponse> invalid(InvalidRoomException e) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("INVALID_ROOM_LAYOUT", e.getMessage()));
    }

    @ExceptionHandler(RoomConflictException.class)
    public ResponseEntity<ApiErrorResponse> conflict(RoomConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("ROOM_VERSION_CONFLICT", e.getMessage()));
    }
}
