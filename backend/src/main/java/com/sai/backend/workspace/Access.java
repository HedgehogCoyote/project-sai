package com.sai.backend.workspace;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sai.backend.auth.exception.UnauthorizedException;
import com.sai.backend.common.session.SessionConst;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Session and space-role checks shared by the workspace controllers. */
@Component
@RequiredArgsConstructor
public class Access {
    public static final String OWNER = "OWNER", MANAGER = "MANAGER", MEMBER = "MEMBER";
    private final JdbcTemplate jdbc;

    public static Long userId(HttpSession session) {
        Long id = (Long) session.getAttribute(SessionConst.LOGIN_USER_ID);
        if (id == null) throw new UnauthorizedException();
        return id;
    }

    /** The caller's role in the space, or 403 when they are not a member. */
    public String role(Long userId, Long spaceId) {
        List<String> roles = jdbc.queryForList(
                "SELECT role FROM space_member WHERE user_id = ? AND space_id = ?", String.class, userId, spaceId);
        if (roles.isEmpty()) throw ApiException.forbidden("이 공간에 접근할 권한이 없습니다.");
        return roles.get(0);
    }

    public String manager(Long userId, Long spaceId) {
        String role = role(userId, spaceId);
        if (MEMBER.equals(role)) throw ApiException.forbidden("소유자나 관리자만 할 수 있습니다.");
        return role;
    }

    public void owner(Long userId, Long spaceId) {
        if (!OWNER.equals(role(userId, spaceId))) throw ApiException.forbidden("소유자만 할 수 있습니다.");
    }

    public String displayName(Long userId, Long spaceId) {
        List<String> names = jdbc.queryForList("""
                SELECT COALESCE(sm.nickname, u.name) FROM users u
                LEFT JOIN space_member sm ON sm.user_id = u.id AND sm.space_id = ?
                WHERE u.id = ?""", String.class, spaceId, userId);
        return names.isEmpty() ? "알 수 없는 사용자" : names.get(0);
    }

    public static String text(String value, int max, String label) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) throw ApiException.bad(label + "을(를) 입력해 주세요.");
        if (v.codePointCount(0, v.length()) > max) throw ApiException.bad(label + "은(는) " + max + "자 이하여야 합니다.");
        return v;
    }
}
