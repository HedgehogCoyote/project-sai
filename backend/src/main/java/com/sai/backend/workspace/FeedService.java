package com.sai.backend.workspace;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Space activity feed. One table answers three questions: what is new in a space
 * (recent news), what is new across my spaces (notifications), and which tool has
 * unread changes (furniture badges). "Unread" = written by someone else after the
 * member last opened the space.
 */
@Service
@RequiredArgsConstructor
public class FeedService {
    public record FeedItem(Long id, Long spaceId, String spaceTitle, String kind, String module, String summary,
                           Long actorId, String actorName, OffsetDateTime createdAt, boolean unread) {}
    public record Unread(Long spaceId, long total, Map<String, Long> modules) {}

    private final JdbcTemplate jdbc;

    public void record(Long spaceId, Long actorId, String kind, String module, String summary) {
        String s = summary == null ? "" : summary.strip();
        if (s.isEmpty()) return;
        if (s.length() > 200) s = s.substring(0, 199) + "…";
        // Collapse bursts (many saves or uploads in a row) into one entry.
        Integer recent = jdbc.queryForObject("""
                SELECT COUNT(*) FROM space_feed WHERE space_id = ? AND actor_id = ? AND kind = ?
                  AND summary = ? AND created_at > CURRENT_TIMESTAMP - INTERVAL '10 minutes'""",
                Integer.class, spaceId, actorId, kind, s);
        if (recent != null && recent > 0) return;
        jdbc.update("""
                INSERT INTO space_feed (space_id, actor_id, kind, module, summary, created_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""", spaceId, actorId, kind, module, s);
    }

    private static final String SELECT = """
            SELECT f.id, f.space_id, s.title, f.kind, f.module, f.summary, f.actor_id,
                   COALESCE(am.nickname, au.name, '알 수 없는 사용자') AS actor_name, f.created_at,
                   (f.actor_id IS DISTINCT FROM sm.user_id
                    AND f.created_at > COALESCE(sm.last_seen_at, CAST(sm.joined_at AS TIMESTAMPTZ))) AS unread
            FROM space_feed f
            JOIN space s ON s.id = f.space_id
            JOIN space_member sm ON sm.space_id = f.space_id AND sm.user_id = ?
            LEFT JOIN users au ON au.id = f.actor_id
            LEFT JOIN space_member am ON am.space_id = f.space_id AND am.user_id = f.actor_id
            """;

    private FeedItem map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new FeedItem(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), (Long) rs.getObject(7), rs.getString(8),
                rs.getObject(9, OffsetDateTime.class), rs.getBoolean(10));
    }

    public List<FeedItem> space(Long userId, Long spaceId, int limit) {
        return jdbc.query(SELECT + " WHERE f.space_id = ? ORDER BY f.created_at DESC, f.id DESC LIMIT ?",
                (rs, i) -> map(rs), userId, spaceId, Math.clamp(limit, 1, 100));
    }

    public List<FeedItem> mine(Long userId, int limit) {
        return jdbc.query(SELECT + " WHERE f.actor_id IS DISTINCT FROM ? ORDER BY f.created_at DESC, f.id DESC LIMIT ?",
                (rs, i) -> map(rs), userId, userId, Math.clamp(limit, 1, 100));
    }

    public List<Unread> unread(Long userId) {
        Map<Long, Unread> bySpace = new LinkedHashMap<>();
        jdbc.query("""
                SELECT sm.space_id, f.module, COUNT(f.id)
                FROM space_member sm
                LEFT JOIN space_feed f ON f.space_id = sm.space_id
                  AND f.actor_id IS DISTINCT FROM sm.user_id
                  AND f.created_at > COALESCE(sm.last_seen_at, CAST(sm.joined_at AS TIMESTAMPTZ))
                WHERE sm.user_id = ?
                GROUP BY sm.space_id, f.module ORDER BY sm.space_id""", rs -> {
            long spaceId = rs.getLong(1), count = rs.getLong(3);
            String module = rs.getString(2);
            Unread u = bySpace.computeIfAbsent(spaceId, id -> new Unread(id, 0, new LinkedHashMap<>()));
            if (count > 0) {
                if (module != null) u.modules().merge(module, count, Long::sum);
                bySpace.put(spaceId, new Unread(spaceId, u.total() + count, u.modules()));
            }
        }, userId);
        return List.copyOf(bySpace.values());
    }

    public void seen(Long userId, Long spaceId) {
        jdbc.update("UPDATE space_member SET last_seen_at = CURRENT_TIMESTAMP WHERE user_id = ? AND space_id = ?",
                userId, spaceId);
    }
}
