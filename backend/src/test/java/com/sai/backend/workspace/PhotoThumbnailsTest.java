package com.sai.backend.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class PhotoThumbnailsTest {

    private static byte[] jpeg(int w, int h) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "jpeg", out);
        return out.toByteArray();
    }

    /** Inserts an APP1 Exif segment (big-endian TIFF, one IFD entry: orientation) after SOI. */
    private static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, 0, 0, 0, 0};
        int len = 2 + 6 + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xff); out.write(0xe1); out.write(len >> 8); out.write(len & 0xff);
        out.writeBytes(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        out.writeBytes(tiff);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    @Test
    void scalesDownAndKeepsAspect() throws Exception {
        BufferedImage t = ImageIO.read(new ByteArrayInputStream(PhotoThumbnails.make(jpeg(2000, 1000))));
        assertThat(t.getWidth()).isEqualTo(480);
        assertThat(t.getHeight()).isEqualTo(240);
    }

    @Test
    void portraitPhotosTaggedRotatedStayUpright() throws Exception {
        byte[] rotated = withOrientation(jpeg(1200, 600), 6);
        assertThat(PhotoThumbnails.orientation(rotated)).isEqualTo(6);
        BufferedImage t = ImageIO.read(new ByteArrayInputStream(PhotoThumbnails.make(rotated)));
        assertThat(t.getWidth()).isEqualTo(240);
        assertThat(t.getHeight()).isEqualTo(480);
    }

    @Test
    void undecodableBytesGiveNoThumbnail() {
        assertThat(PhotoThumbnails.make("RIFF....WEBPVP8 ".getBytes())).isNull();
        assertThat(PhotoThumbnails.orientation(new byte[] {1, 2, 3})).isEqualTo(1);
    }
}
