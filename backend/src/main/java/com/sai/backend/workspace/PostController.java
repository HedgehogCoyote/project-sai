package com.sai.backend.workspace;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Notices (pinned announcements) and comments on activities, photos, records and schedule items. */
@RestController
@RequestMapping("/api/spaces/{spaceId}")
@RequiredArgsConstructor
public class PostController {
    public record PostItem(Long id, String kind, String target, String body, boolean pinned, Long authorId,
                           String authorName, OffsetDateTime createdAt, boolean mine,
                           Map<String, Long> reactions, List<String> myReactions) {}
    public record ReactionRequest(String emoji) {}
    public static final List<String> EMOJIS = List.of("👍", "❤️", "😂", "😮", "🎉");
    public record NoticeRequest(String body, Boolean pinned) {}
    public record CommentRequest(String target, String body) {}

    private static final String TARGET = "^[a-z]{2,20}:[A-Za-z0-9_.-]{1,100}$";
    private final JdbcTemplate jdbc;
    private final Access access;
    private final FeedService feed;

    private List<PostItem> query(Long userId, String where, Object... args) {
        Object[] all = new Object[args.length + 1];
        all[0] = userId;
        System.arraycopy(args, 0, all, 1, args.length);
        List<PostItem> posts = jdbc.query("""
                SELECT p.id, p.kind, p.target, p.body, p.pinned, p.author_id, COALESCE(m.nickname, u.name),
                       p.created_at, p.author_id = ?
                FROM space_post p JOIN users u ON u.id = p.author_id
                LEFT JOIN space_member m ON m.space_id = p.space_id AND m.user_id = p.author_id
                """ + where, (rs, i) -> new PostItem(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getBoolean(5), rs.getLong(6), rs.getString(7), rs.getObject(8, OffsetDateTime.class),
                rs.getBoolean(9), new LinkedHashMap<>(), new java.util.ArrayList<>()), all);
        if (posts.isEmpty()) return posts;
        // Reactions for the listed posts in one query, kept in the fixed emoji order.
        Map<Long, PostItem> byId = new LinkedHashMap<>();
        posts.forEach(p -> byId.put(p.id(), p));
        String ids = String.join(",", byId.keySet().stream().map(String::valueOf).toList());
        jdbc.query("SELECT post_id, emoji, COUNT(*), BOOL_OR(user_id = ?) FROM space_reaction WHERE post_id IN (" + ids
                + ") GROUP BY post_id, emoji", rs -> {
            PostItem p = byId.get(rs.getLong(1));
            p.reactions().put(rs.getString(2), rs.getLong(3));
            if (rs.getBoolean(4)) p.myReactions().add(rs.getString(2));
        }, userId);
        return posts;
    }

    @PostMapping("/posts/{postId}/reactions")
    @Transactional
    public PostItem react(@PathVariable Long spaceId, @PathVariable Long postId, @RequestBody ReactionRequest req,
                          HttpSession session) {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        if (req == null || !EMOJIS.contains(req.emoji())) throw ApiException.bad("고를 수 있는 반응이 아닙니다.");
        List<PostItem> found = query(userId, "WHERE p.id = ? AND p.space_id = ?", postId, spaceId);
        if (found.isEmpty()) throw ApiException.notFound("글을 찾을 수 없습니다.");
        int removed = jdbc.update("DELETE FROM space_reaction WHERE post_id = ? AND user_id = ? AND emoji = ?",
                postId, userId, req.emoji());
        if (removed == 0)
            jdbc.update("INSERT INTO space_reaction (post_id, user_id, emoji, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                    + "ON CONFLICT DO NOTHING", postId, userId, req.emoji());
        return query(userId, "WHERE p.id = ?", postId).get(0);
    }

    @GetMapping("/notices")
    public List<PostItem> notices(@PathVariable Long spaceId, HttpSession session) {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        return query(userId, "WHERE p.space_id = ? AND p.kind = 'NOTICE' ORDER BY p.pinned DESC, p.created_at DESC LIMIT 50", spaceId);
    }

    @PostMapping("/notices")
    @Transactional
    public ResponseEntity<PostItem> notice(@PathVariable Long spaceId, @RequestBody NoticeRequest req, HttpSession session) {
        Long userId = Access.userId(session);
        access.manager(userId, spaceId);
        String body = Access.text(req == null ? null : req.body(), 2000, "공지 내용");
        boolean pinned = req.pinned() != null && req.pinned();
        if (pinned) jdbc.update("UPDATE space_post SET pinned = FALSE WHERE space_id = ? AND kind = 'NOTICE'", spaceId);
        Long id = jdbc.queryForObject("""
                INSERT INTO space_post (space_id, author_id, kind, body, pinned, created_at)
                VALUES (?, ?, 'NOTICE', ?, ?, CURRENT_TIMESTAMP) RETURNING id""", Long.class, spaceId, userId, body, pinned);
        feed.record(spaceId, userId, "NOTICE", "notices", "공지: " + firstLine(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(query(userId, "WHERE p.id = ?", id).get(0));
    }

    @GetMapping("/comments")
    public List<PostItem> comments(@PathVariable Long spaceId, @RequestParam String target, HttpSession session) {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        if (!target.matches(TARGET)) throw ApiException.bad("댓글 대상을 확인해 주세요.");
        return query(userId, "WHERE p.space_id = ? AND p.kind = 'COMMENT' AND p.target = ? ORDER BY p.created_at, p.id LIMIT 200",
                spaceId, target);
    }

    @GetMapping("/comment-counts")
    public Map<String, Long> counts(@PathVariable Long spaceId, HttpSession session) {
        access.role(Access.userId(session), spaceId);
        Map<String, Long> out = new LinkedHashMap<>();
        jdbc.query("SELECT target, COUNT(*) FROM space_post WHERE space_id = ? AND kind = 'COMMENT' GROUP BY target",
                rs -> { out.put(rs.getString(1), rs.getLong(2)); }, spaceId);
        return out;
    }

    @PostMapping("/comments")
    @Transactional
    public ResponseEntity<PostItem> comment(@PathVariable Long spaceId, @RequestBody CommentRequest req, HttpSession session) {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        if (req == null || req.target() == null || !req.target().matches(TARGET)) throw ApiException.bad("댓글 대상을 확인해 주세요.");
        String body = Access.text(req.body(), 2000, "댓글");
        Long id = jdbc.queryForObject("""
                INSERT INTO space_post (space_id, author_id, kind, target, body, created_at)
                VALUES (?, ?, 'COMMENT', ?, ?, CURRENT_TIMESTAMP) RETURNING id""", Long.class, spaceId, userId, req.target(), body);
        String module = req.target().substring(0, req.target().indexOf(':'));
        feed.record(spaceId, userId, "COMMENT", module, access.displayName(userId, spaceId) + "님의 댓글: " + firstLine(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(query(userId, "WHERE p.id = ?", id).get(0));
    }

    @DeleteMapping("/posts/{postId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long spaceId, @PathVariable Long postId, HttpSession session) {
        Long userId = Access.userId(session);
        String role = access.role(userId, spaceId);
        List<Long> authors = jdbc.queryForList("SELECT author_id FROM space_post WHERE id = ? AND space_id = ?",
                Long.class, postId, spaceId);
        if (authors.isEmpty()) throw ApiException.notFound("글을 찾을 수 없습니다.");
        if (!authors.get(0).equals(userId) && Access.MEMBER.equals(role))
            throw ApiException.forbidden("작성자나 관리자만 지울 수 있습니다.");
        jdbc.update("DELETE FROM space_post WHERE id = ?", postId);
        return ResponseEntity.noContent().build();
    }

    private static String firstLine(String body) {
        String line = body.lines().findFirst().orElse("");
        return line.length() > 60 ? line.substring(0, 59) + "…" : line;
    }
}
