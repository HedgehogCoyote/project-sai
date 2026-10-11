package com.sai.backend.room.exception;

public class RoomConflictException extends RuntimeException {
    public RoomConflictException() {
        super("다른 사용자가 방 배치를 변경했습니다. 최신 배치를 조회한 뒤 다시 저장해 주세요.");
    }
}
