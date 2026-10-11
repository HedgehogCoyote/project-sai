package com.sai.backend.room.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sai.backend.room.dto.RoomLayout;
import com.sai.backend.room.dto.RoomResponse;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class RoomRepository {
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public Optional<RoomResponse> find(Long spaceId) {
        return jdbc.query("""
                SELECT space_id, version, layout::text, updated_by, updated_at
                FROM space_room WHERE space_id = ?
                """, (rs, row) -> new RoomResponse(rs.getLong("space_id"), rs.getLong("version"),
                mapper.readValue(rs.getString("layout"), RoomLayout.class),
                rs.getLong("updated_by"), rs.getObject("updated_at", OffsetDateTime.class)), spaceId)
                .stream().findFirst();
    }

    public boolean save(Long spaceId, Long userId, long expectedVersion, RoomLayout layout) {
        String json = mapper.writeValueAsString(layout);
        if (expectedVersion == 0) {
            return jdbc.update("""
                    INSERT INTO space_room (space_id, layout, version, updated_by, updated_at)
                    VALUES (?, CAST(? AS jsonb), 1, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (space_id) DO NOTHING
                    """, spaceId, json, userId) == 1;
        }
        return jdbc.update("""
                UPDATE space_room SET layout = CAST(? AS jsonb), version = version + 1,
                    updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE space_id = ? AND version = ?
                """, json, userId, spaceId, expectedVersion) == 1;
    }
}
