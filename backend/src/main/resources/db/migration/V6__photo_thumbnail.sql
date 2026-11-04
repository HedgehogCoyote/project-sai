-- Small JPEG previews for the album grid; NULL when the format cannot be decoded (e.g. WEBP).
ALTER TABLE space_photo ADD COLUMN thumb_key VARCHAR(160);
