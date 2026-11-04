package com.sai.backend.workspace;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Per-space look and unread counts for the space list, in one call. */
@RestController
@RequiredArgsConstructor
public class SpaceOverviewController {
    public record Overview(Long spaceId, String relation, String colorPrimary, String colorSecondary,
                           long unread, Map<String, Long> modules, boolean muted) {}

    private final JdbcTemplate jdbc;
    private final FeedService feed;

    @GetMapping("/api/spaces/overview")
    public List<Overview> overview(HttpSession session) {
        Long userId = Access.userId(session);
        Map<Long, FeedService.Unread> unread = feed.unread(userId).stream()
                .collect(java.util.stream.Collectors.toMap(FeedService.Unread::spaceId, u -> u));
        return jdbc.query("""
                SELECT s.id, s.relation, s.color_primary, s.color_secondary FROM space s
                JOIN space_member m ON m.space_id = s.id AND m.user_id = ? ORDER BY s.id""", (rs, i) -> {
            FeedService.Unread u = unread.get(rs.getLong(1));
            return new Overview(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    u == null ? 0 : u.total(), u == null ? Map.of() : u.modules(), u != null && u.muted());
        }, userId);
    }
}
