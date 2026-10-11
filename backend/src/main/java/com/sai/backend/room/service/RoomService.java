package com.sai.backend.room.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sai.backend.room.dto.RoomLayout;
import com.sai.backend.room.dto.RoomResponse;
import com.sai.backend.room.dto.SaveRoomRequest;
import com.sai.backend.room.exception.InvalidRoomException;
import com.sai.backend.room.exception.RoomConflictException;
import com.sai.backend.room.repository.RoomRepository;
import com.sai.backend.space.domain.SpaceMember;
import com.sai.backend.space.domain.SpaceMemberRole;
import com.sai.backend.space.exception.SpacePermissionDeniedException;
import com.sai.backend.space.repository.SpaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final SpaceMemberRepository members;
    private final RoomRepository rooms;
    private final RoomLayoutValidator validator;

    @Transactional(readOnly = true)
    public RoomResponse get(Long userId, Long spaceId) {
        member(userId, spaceId);
        return rooms.find(spaceId).orElseGet(() ->
                new RoomResponse(spaceId, 0, RoomLayout.empty(), null, null));
    }

    @Transactional
    public RoomResponse save(Long userId, Long spaceId, SaveRoomRequest request) {
        SpaceMember member = member(userId, spaceId);
        if (member.getRole() != SpaceMemberRole.OWNER && member.getRole() != SpaceMemberRole.MANAGER)
            throw new SpacePermissionDeniedException();
        if (request == null || request.version() == null || request.version() < 0
                || request.version() == Long.MAX_VALUE)
            throw new InvalidRoomException("저장할 배치의 version은 0 이상의 유효한 정수여야 합니다.");
        validator.validate(request.layout());
        if (!rooms.save(spaceId, userId, request.version(), request.layout()))
            throw new RoomConflictException();
        return rooms.find(spaceId).orElseThrow();
    }

    private SpaceMember member(Long userId, Long spaceId) {
        return members.findByUserIdAndSpaceId(userId, spaceId)
                .orElseThrow(SpacePermissionDeniedException::new);
    }
}
