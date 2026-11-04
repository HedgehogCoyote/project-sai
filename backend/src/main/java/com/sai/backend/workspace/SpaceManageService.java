package com.sai.backend.workspace;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpaceManageService {
    public static final Set<String> RELATIONS = Set.of("COUPLE", "FRIENDS", "CLUB", "CUSTOM");
    public record SpaceDetail(Long spaceId, String title, String relation, String colorPrimary,
                              String colorSecondary, String myRole, long memberCount, boolean muted) {}
    public record MemberItem(Long userId, String loginId, String name, String nickname, String role,
                             OffsetDateTime joinedAt) {}
    public record UpdateSpace(String title, String relation, String colorPrimary, String colorSecondary) {}

    private final JdbcTemplate jdbc;
    private final Access access;
    private final FeedService feed;
    private final PhotoStorage storage;

    @Transactional(readOnly = true)
    public SpaceDetail detail(Long userId, Long spaceId) {
        String role = access.role(userId, spaceId);
        return jdbc.queryForObject("""
                SELECT s.id, s.title, s.relation, s.color_primary, s.color_secondary,
                       (SELECT COUNT(*) FROM space_member m WHERE m.space_id = s.id),
                       (SELECT muted FROM space_member m WHERE m.space_id = s.id AND m.user_id = ?)
                FROM space s WHERE s.id = ?""", (rs, i) -> new SpaceDetail(rs.getLong(1), rs.getString(2),
                rs.getString(3), rs.getString(4), rs.getString(5), role, rs.getLong(6), rs.getBoolean(7)), userId, spaceId);
    }

    @Transactional
    public SpaceDetail update(Long userId, Long spaceId, UpdateSpace req) {
        access.manager(userId, spaceId);
        if (req == null) throw ApiException.bad("바꿀 내용을 보내 주세요.");
        if (req.title() != null)
            jdbc.update("UPDATE space SET title = ? WHERE id = ?", Access.text(req.title(), 50, "공간 이름"), spaceId);
        if (req.relation() != null) {
            if (!RELATIONS.contains(req.relation())) throw ApiException.bad("관계 유형을 확인해 주세요.");
            jdbc.update("UPDATE space SET relation = ? WHERE id = ?", req.relation(), spaceId);
        }
        if (req.colorPrimary() != null || req.colorSecondary() != null) {
            String a = color(req.colorPrimary()), b = color(req.colorSecondary());
            jdbc.update("UPDATE space SET color_primary = COALESCE(?, color_primary), "
                    + "color_secondary = COALESCE(?, color_secondary) WHERE id = ?", a, b, spaceId);
        }
        feed.record(spaceId, userId, "SPACE", null, access.displayName(userId, spaceId) + "님이 공간 설정을 바꿨습니다.");
        return detail(userId, spaceId);
    }

    private static String color(String c) {
        if (c == null) return null;
        String v = c.strip().toLowerCase();
        if (!v.matches("^#[0-9a-f]{6}$")) throw ApiException.bad("색은 #rrggbb 형식이어야 합니다.");
        return v;
    }

    @Transactional(readOnly = true)
    public List<MemberItem> members(Long userId, Long spaceId) {
        access.role(userId, spaceId);
        return jdbc.query("""
                SELECT u.id, u.login_id, u.name, m.nickname, m.role, CAST(m.joined_at AS TIMESTAMPTZ)
                FROM space_member m JOIN users u ON u.id = m.user_id
                WHERE m.space_id = ?
                ORDER BY CASE m.role WHEN 'OWNER' THEN 0 WHEN 'MANAGER' THEN 1 ELSE 2 END, m.joined_at, u.id""",
                (rs, i) -> new MemberItem(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getObject(6, OffsetDateTime.class)), spaceId);
    }

    @Transactional
    public void nickname(Long userId, Long spaceId, String nickname) {
        access.role(userId, spaceId);
        String v = nickname == null || nickname.isBlank() ? null : Access.text(nickname, 30, "공간 닉네임");
        jdbc.update("UPDATE space_member SET nickname = ? WHERE user_id = ? AND space_id = ?", v, userId, spaceId);
    }

    @Transactional
    public void mute(Long userId, Long spaceId, boolean muted) {
        access.role(userId, spaceId);
        jdbc.update("UPDATE space_member SET muted = ? WHERE user_id = ? AND space_id = ?", muted, userId, spaceId);
    }

    /** OWNER changes roles. Giving OWNER to someone transfers ownership; the old owner becomes MANAGER. */
    @Transactional
    public void changeRole(Long userId, Long spaceId, Long targetId, String role) {
        access.owner(userId, spaceId);
        if (userId.equals(targetId)) throw ApiException.bad("자신의 역할은 바꿀 수 없습니다. 다른 멤버에게 소유권을 넘겨 주세요.");
        if (!Set.of(Access.OWNER, Access.MANAGER, Access.MEMBER).contains(role)) throw ApiException.bad("역할을 확인해 주세요.");
        access.role(targetId, spaceId);
        jdbc.update("UPDATE space_member SET role = ? WHERE user_id = ? AND space_id = ?", role, targetId, spaceId);
        String target = access.displayName(targetId, spaceId);
        if (Access.OWNER.equals(role)) {
            jdbc.update("UPDATE space_member SET role = 'MANAGER' WHERE user_id = ? AND space_id = ?", userId, spaceId);
            feed.record(spaceId, userId, "MEMBER", null, target + "님이 새 소유자가 되었습니다.");
        } else {
            feed.record(spaceId, userId, "MEMBER", null, target + "님의 역할이 " + (Access.MANAGER.equals(role) ? "관리자" : "멤버") + "로 바뀌었습니다.");
        }
    }

    /** OWNER removes anyone but themselves; MANAGER removes MEMBERs only. */
    @Transactional
    public void remove(Long userId, Long spaceId, Long targetId) {
        String mine = access.manager(userId, spaceId);
        if (userId.equals(targetId)) throw ApiException.bad("공간을 나가려면 나가기를 사용해 주세요.");
        String theirs = access.role(targetId, spaceId);
        if (Access.OWNER.equals(theirs) || (Access.MANAGER.equals(mine) && !Access.MEMBER.equals(theirs)))
            throw ApiException.forbidden("이 멤버를 내보낼 권한이 없습니다.");
        String name = access.displayName(targetId, spaceId);
        jdbc.update("DELETE FROM space_member WHERE user_id = ? AND space_id = ?", targetId, spaceId);
        feed.record(spaceId, userId, "MEMBER", null, name + "님을 공간에서 내보냈습니다.");
    }

    /** Leave a space. The last member leaving deletes it; an owner with others must transfer first. */
    @Transactional
    public boolean leave(Long userId, Long spaceId) {
        String role = access.role(userId, spaceId);
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM space_member WHERE space_id = ?", Long.class, spaceId);
        if (count <= 1) { deleteSpace(spaceId); return true; }
        if (Access.OWNER.equals(role))
            throw ApiException.conflict("OWNER_MUST_TRANSFER", "다른 멤버에게 소유권을 넘긴 뒤 나갈 수 있습니다.");
        String name = access.displayName(userId, spaceId);
        jdbc.update("DELETE FROM space_member WHERE user_id = ? AND space_id = ?", userId, spaceId);
        feed.record(spaceId, userId, "MEMBER", null, name + "님이 공간을 나갔습니다.");
        return false;
    }

    @Transactional
    public void delete(Long userId, Long spaceId) {
        access.owner(userId, spaceId);
        deleteSpace(spaceId);
    }

    /** Tables without ON DELETE CASCADE are cleared first; photo files are removed after commit-safe deletes. */
    @Transactional
    public void deleteSpace(Long spaceId) {
        List<String> keys = jdbc.queryForList("""
                SELECT storage_key FROM space_photo WHERE space_id = ?
                UNION ALL SELECT thumb_key FROM space_photo WHERE space_id = ? AND thumb_key IS NOT NULL""",
                String.class, spaceId, spaceId);
        jdbc.update("DELETE FROM space_invitation WHERE space_id = ?", spaceId);
        jdbc.update("DELETE FROM space_room WHERE space_id = ?", spaceId);
        jdbc.update("DELETE FROM space_member WHERE space_id = ?", spaceId);
        jdbc.update("DELETE FROM space WHERE id = ?", spaceId);
        keys.forEach(storage::delete);
    }
}
