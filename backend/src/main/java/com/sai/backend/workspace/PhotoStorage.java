package com.sai.backend.workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Photo files on local disk. Keys are "<spaceId>/<uuid>.<ext>" and never come from the client. */
@Component
public class PhotoStorage {
    private final Path root;

    public PhotoStorage(@Value("${sai.storage.dir:data/photos}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    public String save(Long spaceId, byte[] bytes, String ext) {
        String key = spaceId + "/" + UUID.randomUUID() + "." + ext;
        Path path = resolve(key);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 저장하지 못했습니다.", e);
        }
        return key;
    }

    public Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw ApiException.bad("잘못된 사진 경로입니다.");
        return path;
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // A missing file must not block deleting its record.
        }
    }
}
