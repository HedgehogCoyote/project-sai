package com.sai.backend.workspace;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Profile name, password change and account deletion. */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    public record ProfileRequest(String name) {}
    public record PasswordRequest(String currentPassword, String newPassword) {}
    public record DeleteRequest(String password) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SpaceManageService spaces;

    @PatchMapping("/profile")
    public ResponseEntity<Void> profile(@RequestBody ProfileRequest req, HttpSession session) {
        Long userId = Access.userId(session);
        jdbc.update("UPDATE users SET name = ? WHERE id = ? AND deleted_at IS NULL",
                Access.text(req == null ? null : req.name(), 50, "이름"), userId);
        return ResponseEntity.noContent().build();
    }

    private void verify(Long userId, String password) {
        List<String> hashes = jdbc.queryForList("SELECT password_hash FROM users WHERE id = ? AND deleted_at IS NULL",
                String.class, userId);
        if (hashes.isEmpty() || password == null || !encoder.matches(password, hashes.get(0)))
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "WRONG_PASSWORD",
                    "현재 비밀번호가 맞지 않습니다.");
    }

    @PostMapping("/password")
    public ResponseEntity<Void> password(@RequestBody PasswordRequest req, HttpSession session) {
        Long userId = Access.userId(session);
        if (req == null) throw ApiException.bad("비밀번호를 입력해 주세요.");
        verify(userId, req.currentPassword());
        String next = req.newPassword() == null ? "" : req.newPassword();
        if (next.length() < 8 || next.length() > 30) throw ApiException.bad("새 비밀번호는 8자 이상 30자 이하여야 합니다.");
        if (next.equals(req.currentPassword())) throw ApiException.bad("지금과 다른 비밀번호를 입력해 주세요.");
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", encoder.encode(next), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the account: spaces where the user is alone are deleted, memberships are
     * removed and personal fields are cleared. Records others can still see keep the
     * row under the name "탈퇴한 사용자".
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> delete(@RequestBody DeleteRequest req, HttpSession session) {
        Long userId = Access.userId(session);
        verify(userId, req == null ? null : req.password());
        Integer owned = jdbc.queryForObject("""
                SELECT COUNT(*) FROM space_member m WHERE m.user_id = ? AND m.role = 'OWNER'
                  AND (SELECT COUNT(*) FROM space_member o WHERE o.space_id = m.space_id) > 1""", Integer.class, userId);
        if (owned != null && owned > 0)
            throw ApiException.conflict("OWNER_MUST_TRANSFER",
                    "소유자인 공간 " + owned + "곳의 소유권을 다른 멤버에게 넘긴 뒤 탈퇴할 수 있습니다.");
        jdbc.queryForList("""
                SELECT m.space_id FROM space_member m
                WHERE m.user_id = ? AND (SELECT COUNT(*) FROM space_member o WHERE o.space_id = m.space_id) = 1""",
                Long.class, userId).forEach(spaces::deleteSpace);
        jdbc.update("DELETE FROM space_member WHERE user_id = ?", userId);
        jdbc.update("UPDATE space_invitation SET status = 'DENIED' WHERE status = 'PENDING' AND (inviter_id = ? OR invitee_id = ?)",
                userId, userId);
        jdbc.update("UPDATE space_invite_link SET revoked_at = CURRENT_TIMESTAMP WHERE created_by = ? AND revoked_at IS NULL", userId);
        jdbc.update("""
                UPDATE users SET name = '탈퇴한 사용자', login_id = ?, password_hash = ?, email = NULL,
                    phone_number = NULL, deleted_at = CURRENT_TIMESTAMP WHERE id = ?""",
                "deleted-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8),
                encoder.encode(UUID.randomUUID().toString()), userId);
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
