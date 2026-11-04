package com.sai.backend.workspace;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared space content: activities and their tools (records, tasks, schedule, trip
 * plan, packing, expenses, polls, Foot Prints) as one versioned JSON document.
 * Every member may write; a stale version gets 409 and the client re-applies its
 * change on the latest document.
 */
@RestController
@RequestMapping("/api/spaces/{spaceId}/content")
@RequiredArgsConstructor
public class ContentController {
    public record ContentResponse(long version, JsonNode content, OffsetDateTime updatedAt, Long updatedBy,
                                  String updatedByName) {}
    public record SaveContent(Long version, JsonNode content, String summary, String module) {}

    private static final int MAX_CHARS = 2_000_000;
    private final JdbcTemplate jdbc;
    private final Access access;
    private final FeedService feed;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @GetMapping
    @Transactional(readOnly = true)
    public ContentResponse get(@PathVariable Long spaceId, HttpSession session) {
        access.role(Access.userId(session), spaceId);
        return find(spaceId);
    }

    private ContentResponse find(Long spaceId) {
        List<ContentResponse> rows = jdbc.query("""
                SELECT c.version, c.content::text, c.updated_at, c.updated_by, COALESCE(m.nickname, u.name)
                FROM space_content c JOIN users u ON u.id = c.updated_by
                LEFT JOIN space_member m ON m.space_id = c.space_id AND m.user_id = c.updated_by
                WHERE c.space_id = ?""", (rs, i) -> new ContentResponse(rs.getLong(1), mapper.readTree(rs.getString(2)),
                rs.getObject(3, OffsetDateTime.class), rs.getLong(4), rs.getString(5)), spaceId);
        return rows.isEmpty() ? new ContentResponse(0, null, null, null, null) : rows.get(0);
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ContentResponse save(@PathVariable Long spaceId, @RequestBody String body, HttpSession session) {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        if (body.length() > MAX_CHARS) throw ApiException.bad("공간 데이터가 너무 큽니다. 오래된 기록을 정리해 주세요.");
        SaveContent req;
        try {
            req = mapper.readValue(body, SaveContent.class);
        } catch (JacksonException e) {
            throw ApiException.bad("저장할 데이터 형식을 확인해 주세요.");
        }
        if (req.version() == null || req.version() < 0) throw ApiException.bad("version은 0 이상의 정수여야 합니다.");
        JsonNode content = req.content();
        JsonNode v = content == null ? null : content.get("version");
        if (content == null || !content.isObject() || v == null || !v.isIntegralNumber() || v.intValue() != 1)
            throw ApiException.bad("공간 데이터는 version 1 객체여야 합니다.");
        String json = mapper.writeValueAsString(content);
        boolean saved = req.version() == 0
                ? jdbc.update("""
                    INSERT INTO space_content (space_id, content, version, updated_by, updated_at)
                    VALUES (?, CAST(? AS jsonb), 1, ?, CURRENT_TIMESTAMP) ON CONFLICT (space_id) DO NOTHING""",
                    spaceId, json, userId) == 1
                : jdbc.update("""
                    UPDATE space_content SET content = CAST(? AS jsonb), version = version + 1,
                        updated_by = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE space_id = ? AND version = ?""", json, userId, spaceId, req.version()) == 1;
        if (!saved) throw ApiException.conflict("CONTENT_VERSION_CONFLICT",
                "다른 멤버가 먼저 저장했습니다. 최신 내용을 불러와 다시 적용합니다.");
        String module = req.module() != null && req.module().matches("^[a-z]{2,20}$") ? req.module() : null;
        if (req.summary() != null && !req.summary().isBlank())
            feed.record(spaceId, userId, "CONTENT", module,
                    access.displayName(userId, spaceId) + "님이 " + req.summary().strip());
        return find(spaceId);
    }
}
