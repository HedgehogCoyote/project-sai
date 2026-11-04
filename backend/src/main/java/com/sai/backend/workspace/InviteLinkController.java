package com.sai.backend.workspace;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Invite links: one link shared in a group chat lets anyone with an account join. */
@RestController
@RequiredArgsConstructor
public class InviteLinkController {
    public record LinkItem(Long id, String token, OffsetDateTime createdAt, OffsetDateTime expiresAt, String createdByName) {}
    public record LinkPreview(String status, Long spaceId, String spaceTitle, String relation, String colorPrimary,
                              String colorSecondary, long memberCount, String inviterName, OffsetDateTime expiresAt,
                              boolean alreadyMember) {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final Access access;

    @PostMapping("/api/spaces/{spaceId}/invite-links")
    @Transactional
    public ResponseEntity<LinkItem> create(@PathVariable Long spaceId, HttpSession session) {
        Long userId = Access.userId(session);
        access.manager(userId, spaceId);
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Long id = jdbc.queryForObject("""
                INSERT INTO space_invite_link (space_id, token, created_by, created_at, expires_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days') RETURNING id""",
                Long.class, spaceId, token, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(list(spaceId).stream()
                .filter(l -> l.id().equals(id)).findFirst().orElseThrow());
    }

    @GetMapping("/api/spaces/{spaceId}/invite-links")
    public List<LinkItem> active(@PathVariable Long spaceId, HttpSession session) {
        access.manager(Access.userId(session), spaceId);
        return list(spaceId);
    }

    private List<LinkItem> list(Long spaceId) {
        return jdbc.query("""
                SELECT l.id, l.token, l.created_at, l.expires_at, u.name FROM space_invite_link l
                JOIN users u ON u.id = l.created_by
                WHERE l.space_id = ? AND l.revoked_at IS NULL AND l.expires_at > CURRENT_TIMESTAMP
                ORDER BY l.created_at DESC""", (rs, i) -> new LinkItem(rs.getLong(1), rs.getString(2),
                rs.getObject(3, OffsetDateTime.class), rs.getObject(4, OffsetDateTime.class), rs.getString(5)), spaceId);
    }

    @DeleteMapping("/api/spaces/{spaceId}/invite-links/{linkId}")
    public ResponseEntity<Void> revoke(@PathVariable Long spaceId, @PathVariable Long linkId, HttpSession session) {
        access.manager(Access.userId(session), spaceId);
        int n = jdbc.update("UPDATE space_invite_link SET revoked_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND space_id = ? AND revoked_at IS NULL", linkId, spaceId);
        if (n == 0) throw ApiException.notFound("초대 링크를 찾을 수 없습니다.");
        return ResponseEntity.noContent().build();
    }

    /** Public preview so the join page can show where the link leads before login. */
    @GetMapping("/api/invite-links/{token}")
    public LinkPreview preview(@PathVariable String token, HttpSession session) {
        Long viewer = (Long) session.getAttribute(com.sai.backend.common.session.SessionConst.LOGIN_USER_ID);
        List<LinkPreview> found = jdbc.query("""
                SELECT CASE WHEN l.revoked_at IS NOT NULL THEN 'REVOKED'
                            WHEN l.expires_at <= CURRENT_TIMESTAMP THEN 'EXPIRED' ELSE 'VALID' END,
                       s.id, s.title, s.relation, s.color_primary, s.color_secondary,
                       (SELECT COUNT(*) FROM space_member m WHERE m.space_id = s.id), u.name, l.expires_at,
                       EXISTS (SELECT 1 FROM space_member m WHERE m.space_id = s.id AND m.user_id = ?)
                FROM space_invite_link l JOIN space s ON s.id = l.space_id JOIN users u ON u.id = l.created_by
                WHERE l.token = ?""", (rs, i) -> new LinkPreview(rs.getString(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getLong(7), rs.getString(8),
                rs.getObject(9, OffsetDateTime.class), rs.getBoolean(10)), viewer, token);
        if (found.isEmpty()) throw ApiException.notFound("초대 링크를 찾을 수 없습니다. 링크를 다시 확인해 주세요.");
        return found.get(0);
    }

    @PostMapping("/api/invite-links/{token}/join")
    @Transactional
    public Map<String, Object> join(@PathVariable String token, HttpSession session) {
        Long userId = Access.userId(session);
        LinkPreview link = preview(token, session);
        if (link.alreadyMember()) return Map.of("spaceId", link.spaceId(), "alreadyMember", true);
        if (!"VALID".equals(link.status()))
            throw new ApiException(HttpStatus.GONE, "INVITE_LINK_" + link.status(),
                    "REVOKED".equals(link.status()) ? "더 이상 쓸 수 없는 초대 링크입니다." : "기간이 지난 초대 링크입니다. 새 링크를 받아 주세요.");
        jdbc.update("""
                INSERT INTO space_member (user_id, space_id, joined_at, role)
                VALUES (?, ?, CURRENT_TIMESTAMP, 'MEMBER')""", userId, link.spaceId());
        return Map.of("spaceId", link.spaceId(), "alreadyMember", false);
    }
}
