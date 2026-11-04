package com.sai.backend.workspace;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spaces/{spaceId}")
@RequiredArgsConstructor
public class SpaceManageController {
    private final SpaceManageService spaces;
    private final FeedService feed;

    public record RoleRequest(String role) {}
    public record NicknameRequest(String nickname) {}

    @GetMapping
    public SpaceManageService.SpaceDetail detail(@PathVariable Long spaceId, HttpSession session) {
        return spaces.detail(Access.userId(session), spaceId);
    }

    @PatchMapping
    public SpaceManageService.SpaceDetail update(@PathVariable Long spaceId,
                                                 @RequestBody SpaceManageService.UpdateSpace body, HttpSession session) {
        return spaces.update(Access.userId(session), spaceId, body);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long spaceId, HttpSession session) {
        spaces.delete(Access.userId(session), spaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members")
    public List<SpaceManageService.MemberItem> members(@PathVariable Long spaceId, HttpSession session) {
        return spaces.members(Access.userId(session), spaceId);
    }

    @PatchMapping("/members/me")
    public ResponseEntity<Void> nickname(@PathVariable Long spaceId, @RequestBody NicknameRequest body,
                                         HttpSession session) {
        spaces.nickname(Access.userId(session), spaceId, body == null ? null : body.nickname());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/members/{userId}")
    public ResponseEntity<Void> role(@PathVariable Long spaceId, @PathVariable Long userId,
                                     @RequestBody RoleRequest body, HttpSession session) {
        spaces.changeRole(Access.userId(session), spaceId, userId, body == null ? null : body.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{userId}")
    public ResponseEntity<Void> remove(@PathVariable Long spaceId, @PathVariable Long userId, HttpSession session) {
        spaces.remove(Access.userId(session), spaceId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    public Map<String, Boolean> leave(@PathVariable Long spaceId, HttpSession session) {
        return Map.of("spaceDeleted", spaces.leave(Access.userId(session), spaceId));
    }

    @GetMapping("/feed")
    public List<FeedService.FeedItem> feed(@PathVariable Long spaceId,
                                           @RequestParam(defaultValue = "30") int limit, HttpSession session) {
        Long userId = Access.userId(session);
        spaces.detail(userId, spaceId);
        return feed.space(userId, spaceId, limit);
    }

    @PostMapping("/seen")
    public ResponseEntity<Void> seen(@PathVariable Long spaceId, HttpSession session) {
        Long userId = Access.userId(session);
        spaces.detail(userId, spaceId);
        feed.seen(userId, spaceId);
        return ResponseEntity.noContent().build();
    }
}
