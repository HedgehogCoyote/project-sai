package com.sai.backend.workspace;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/** Builds a 480px JPEG preview, honouring the camera's EXIF orientation so portrait photos stay upright. */
final class PhotoThumbnails {
    static final int SIZE = 480;

    private PhotoThumbnails() {}

    /** Returns JPEG bytes, or null when the image cannot be decoded (the album then shows the original). */
    static byte[] make(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, Math.max(w, h) / (SIZE * 2)); // decode at reduced size to save memory
                param.setSourceSubsampling(step, step, 0, 0);
                BufferedImage src = reader.read(0, param);
                return encode(orient(scale(src), orientation(bytes)));
            } finally {
                reader.dispose();
            }
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private static BufferedImage scale(BufferedImage src) {
        double k = Math.min(1.0, (double) SIZE / Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * k)), h = Math.max(1, (int) Math.round(src.getHeight() * k));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE); // transparent PNG/GIF areas become white, not black
        g.fillRect(0, 0, w, h);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** EXIF orientation 1–8 from a JPEG's APP1 segment; 1 when absent or not a JPEG. */
    static int orientation(byte[] b) {
        if (b.length < 4 || (b[0] & 0xff) != 0xff || (b[1] & 0xff) != 0xd8) return 1;
        int i = 2;
        while (i + 4 < b.length && (b[i] & 0xff) == 0xff) {
            int marker = b[i + 1] & 0xff, len = ((b[i + 2] & 0xff) << 8) | (b[i + 3] & 0xff);
            if (marker == 0xe1 && i + 10 < b.length && b[i + 4] == 'E' && b[i + 5] == 'x' && b[i + 6] == 'i' && b[i + 7] == 'f') {
                int t = i + 10; // TIFF header
                boolean le = b[t] == 'I';
                int ifd = t + u32(b, t + 4, le);
                int count = u16(b, ifd, le);
                for (int e = 0; e < count; e++) {
                    int entry = ifd + 2 + e * 12;
                    if (entry + 10 > b.length) break;
                    if (u16(b, entry, le) == 0x0112) {
                        int v = u16(b, entry + 8, le);
                        return v >= 1 && v <= 8 ? v : 1;
                    }
                }
                return 1;
            }
            if (marker == 0xda) break; // image data starts; no EXIF before it
            i += 2 + len;
        }
        return 1;
    }

    private static int u16(byte[] b, int i, boolean le) {
        return le ? (b[i] & 0xff) | ((b[i + 1] & 0xff) << 8) : ((b[i] & 0xff) << 8) | (b[i + 1] & 0xff);
    }

    private static int u32(byte[] b, int i, boolean le) {
        return le ? u16(b, i, true) | (u16(b, i + 2, true) << 16) : (u16(b, i, false) << 16) | u16(b, i + 2, false);
    }

    private static BufferedImage orient(BufferedImage img, int o) {
        if (o == 1) return img;
        int w = img.getWidth(), h = img.getHeight();
        boolean swap = o >= 5;
        AffineTransform t = new AffineTransform();
        switch (o) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            case 8 -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            default -> { return img; }
        }
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, t, null);
        g.dispose();
        return out;
    }

    private static byte[] encode(BufferedImage img) throws java.io.IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam p = writer.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.82f);
            writer.write(null, new IIOImage(img, null, null), p);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
