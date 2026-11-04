package com.sai.backend.workspace;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feed;

    /** Notifications: what others did across all of my spaces, newest first. */
    @GetMapping
    public List<FeedService.FeedItem> mine(@RequestParam(defaultValue = "40") int limit, HttpSession session) {
        return feed.mine(Access.userId(session), limit);
    }

    @GetMapping("/unread")
    public List<FeedService.Unread> unread(HttpSession session) {
        return feed.unread(Access.userId(session));
    }
}
