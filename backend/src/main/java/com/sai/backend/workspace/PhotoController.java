package com.sai.backend.workspace;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/** Space photo album. Photos are the raw material of Foot Prints and photo souvenirs. */
@RestController
@RequestMapping("/api/spaces/{spaceId}/photos")
@RequiredArgsConstructor
public class PhotoController {
    public record PhotoItem(Long id, String eventId, String caption, Long uploaderId, String uploaderName,
                            String contentType, long sizeBytes, OffsetDateTime createdAt, String url, boolean mine) {}
    public record PhotoUpdate(String caption, String eventId) {}

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final String EVENT = "^[A-Za-z0-9_-]{1,80}$";
    private final JdbcTemplate jdbc;
    private final Access access;
    private final FeedService feed;
    private final PhotoStorage storage;

    private List<PhotoItem> query(Long userId, Long spaceId, String where, Object... args) {
        Object[] all = new Object[args.length + 1];
        all[0] = userId;
        System.arraycopy(args, 0, all, 1, args.length);
        return jdbc.query("""
                SELECT p.id, p.event_id, p.caption, p.uploader_id, COALESCE(m.nickname, u.name), p.content_type,
                       p.size_bytes, p.created_at, p.uploader_id = ?
                FROM space_photo p JOIN users u ON u.id = p.uploader_id
                LEFT JOIN space_member m ON m.space_id = p.space_id AND m.user_id = p.uploader_id
                """ + where, (rs, i) -> new PhotoItem(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                rs.getString(5), rs.getString(6), rs.getLong(7), rs.getObject(8, OffsetDateTime.class),
                "/api/spaces/" + spaceId + "/photos/" + rs.getLong(1) + "/file", rs.getBoolean(9)), all);
    }

    @GetMapping
    public List<PhotoItem> list(@PathVariable Long spaceId, @RequestParam(required = false) String eventId,
                                HttpSession session) {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        if (eventId != null && !eventId.isBlank())
            return query(userId, spaceId, "WHERE p.space_id = ? AND p.event_id = ? ORDER BY p.created_at DESC, p.id DESC",
                    spaceId, eventId);
        return query(userId, spaceId, "WHERE p.space_id = ? ORDER BY p.created_at DESC, p.id DESC LIMIT 500", spaceId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<PhotoItem> upload(@PathVariable Long spaceId, @RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String eventId,
                                            @RequestParam(required = false) String caption, HttpSession session)
            throws IOException {
        Long userId = Access.userId(session);
        access.role(userId, spaceId);
        if (file.isEmpty()) throw ApiException.bad("올릴 사진을 골라 주세요.");
        if (file.getSize() > MAX_BYTES)
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "사진은 한 장에 10MB까지 올릴 수 있습니다.");
        byte[] bytes = file.getBytes();
        String[] type = sniff(bytes);
        if (type == null) throw ApiException.bad("JPG, PNG, WEBP, GIF 사진만 올릴 수 있습니다.");
        String event = eventId == null || eventId.isBlank() ? null : eventId.strip();
        if (event != null && !event.matches(EVENT)) throw ApiException.bad("활동을 확인해 주세요.");
        String cap = caption == null || caption.isBlank() ? null : Access.text(caption, 200, "사진 설명");
        String key = storage.save(spaceId, bytes, type[1]);
        byte[] thumb = PhotoThumbnails.make(bytes);
        String thumbKey = thumb == null ? null : storage.save(spaceId, thumb, "thumb.jpg");
        Long id = jdbc.queryForObject("""
                INSERT INTO space_photo (space_id, uploader_id, event_id, caption, storage_key, content_type, size_bytes,
                                         thumb_key, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) RETURNING id""",
                Long.class, spaceId, userId, event, cap, key, type[0], (long) bytes.length, thumbKey);
        feed.record(spaceId, userId, "PHOTO", "photos", access.displayName(userId, spaceId) + "님이 사진을 올렸습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(query(userId, spaceId, "WHERE p.id = ?", id).get(0));
    }

    /** Detect the image type from its bytes, never from the client's file name or header. */
    static String[] sniff(byte[] b) {
        if (b.length > 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff)
            return new String[] {"image/jpeg", "jpg"};
        if (b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G')
            return new String[] {"image/png", "png"};
        if (b.length > 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8')
            return new String[] {"image/gif", "gif"};
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')
            return new String[] {"image/webp", "webp"};
        return null;
    }

    private record Stored(Long uploaderId, String key, String contentType, String thumbKey) {}

    private Stored stored(Long spaceId, Long photoId) {
        List<Stored> rows = jdbc.query("SELECT uploader_id, storage_key, content_type, thumb_key FROM space_photo WHERE id = ? AND space_id = ?",
                (rs, i) -> new Stored(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), photoId, spaceId);
        if (rows.isEmpty()) throw ApiException.notFound("사진을 찾을 수 없습니다.");
        return rows.get(0);
    }

    @GetMapping("/{photoId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long spaceId, @PathVariable Long photoId,
                                         @RequestParam(required = false) String size, HttpSession session) {
        access.role(Access.userId(session), spaceId);
        Stored s = stored(spaceId, photoId);
        boolean thumb = "thumb".equals(size) && s.thumbKey() != null;
        FileSystemResource resource = new FileSystemResource(storage.resolve(thumb ? s.thumbKey() : s.key()));
        if (!resource.exists()) throw ApiException.notFound("사진 파일이 없습니다.");
        return ResponseEntity.ok()
                .contentType(thumb ? MediaType.IMAGE_JPEG : MediaType.parseMediaType(s.contentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @PatchMapping("/{photoId}")
    @Transactional
    public PhotoItem update(@PathVariable Long spaceId, @PathVariable Long photoId, @RequestBody PhotoUpdate req,
                            HttpSession session) {
        Long userId = Access.userId(session);
        String role = access.role(userId, spaceId);
        Stored s = stored(spaceId, photoId);
        if (!s.uploaderId().equals(userId) && Access.MEMBER.equals(role))
            throw ApiException.forbidden("올린 사람이나 관리자만 바꿀 수 있습니다.");
        String cap = req.caption() == null || req.caption().isBlank() ? null : Access.text(req.caption(), 200, "사진 설명");
        String event = req.eventId() == null || req.eventId().isBlank() ? null : req.eventId().strip();
        if (event != null && !event.matches(EVENT)) throw ApiException.bad("활동을 확인해 주세요.");
        jdbc.update("UPDATE space_photo SET caption = ?, event_id = ? WHERE id = ?", cap, event, photoId);
        return query(userId, spaceId, "WHERE p.id = ?", photoId).get(0);
    }

    @DeleteMapping("/{photoId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long spaceId, @PathVariable Long photoId, HttpSession session) {
        Long userId = Access.userId(session);
        String role = access.role(userId, spaceId);
        Stored s = stored(spaceId, photoId);
        if (!s.uploaderId().equals(userId) && Access.MEMBER.equals(role))
            throw ApiException.forbidden("올린 사람이나 관리자만 지울 수 있습니다.");
        jdbc.update("DELETE FROM space_photo WHERE id = ?", photoId);
        storage.delete(s.key());
        if (s.thumbKey() != null) storage.delete(s.thumbKey());
        return ResponseEntity.noContent().build();
    }
}
