package com.sai.backend.room.dto;

import java.time.OffsetDateTime;

public record RoomResponse(Long spaceId, long version, RoomLayout layout,
                           Long updatedBy, OffsetDateTime updatedAt) {}
