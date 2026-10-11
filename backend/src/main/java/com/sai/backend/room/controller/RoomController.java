package com.sai.backend.room.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.common.session.SessionConst;
import com.sai.backend.room.dto.RoomResponse;
import com.sai.backend.room.dto.SaveRoomRequest;
import com.sai.backend.room.exception.InvalidRoomException;
import com.sai.backend.room.service.RoomService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/spaces/{spaceId}/room")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService rooms;
    private final JsonMapper requestMapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build();

    @GetMapping
    public RoomResponse get(@PathVariable Long spaceId, HttpSession session) {
        return rooms.get(userId(session), spaceId);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RoomResponse save(@PathVariable Long spaceId, @RequestBody String body,
                             HttpSession session) {
        Long userId = userId(session);
        SaveRoomRequest request;
        try {
            request = requestMapper.readValue(body, SaveRoomRequest.class);
        } catch (JacksonException e) {
            throw new InvalidRoomException("맵 JSON 형식을 확인해 주세요. 좌표·회전·버전은 정수이며 정의된 필드만 허용합니다.");
        }
        return rooms.save(userId, spaceId, request);
    }

    private Long userId(HttpSession session) {
        Long userId = (Long) session.getAttribute(SessionConst.LOGIN_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        return userId;
    }
}
