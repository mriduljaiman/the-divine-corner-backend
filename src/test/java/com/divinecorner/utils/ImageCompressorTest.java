package com.divinecorner.utils;

import com.divinecorner.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ImageCompressorTest {

    private static byte[] png(BufferedImage img) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private static BufferedImage decode(byte[] jpeg) throws Exception {
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertNotNull(out, "output must be a readable image");
        return out;
    }

    @Test
    void randomNoisePhotoIsForcedUnder20Kb() throws Exception {
        // worst case for JPEG: pure noise, 3000x2000
        BufferedImage img = new BufferedImage(3000, 2000, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(42);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, r.nextInt(0xFFFFFF));
            }
        }
        byte[] input = png(img);
        assertTrue(input.length > 1_000_000);

        byte[] out = ImageCompressor.toMaxSizeJpeg(input);
        assertTrue(out.length <= ImageCompressor.MAX_BYTES, "got " + out.length + " bytes");
        BufferedImage result = decode(out);
        assertTrue(result.getWidth() <= 800 && result.getHeight() <= 1067);
    }

    @Test
    void normalProductPhotoKeepsFullSizeAndAspect() throws Exception {
        BufferedImage img = new BufferedImage(2400, 3200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(250, 230, 200), 2400, 3200, new Color(120, 40, 60)));
        g.fillRect(0, 0, 2400, 3200);
        g.setColor(new Color(200, 150, 30));
        g.fillOval(600, 800, 1200, 1600);
        g.dispose();

        byte[] out = ImageCompressor.toMaxSizeJpeg(png(img));
        assertTrue(out.length <= ImageCompressor.MAX_BYTES, "got " + out.length + " bytes");
        BufferedImage result = decode(out);
        assertEquals(800, result.getWidth());
        assertEquals(1067, result.getHeight());
    }

    @Test
    void transparentPngGetsWhiteBackground() throws Exception {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        byte[] out = ImageCompressor.toMaxSizeJpeg(png(img));
        BufferedImage result = decode(out);
        Color c = new Color(result.getRGB(10, 10));
        assertTrue(c.getRed() > 240 && c.getGreen() > 240 && c.getBlue() > 240, "background should be white, was " + c);
    }

    @Test
    void smallImageIsNotUpscaled() throws Exception {
        BufferedImage img = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        BufferedImage result = decode(ImageCompressor.toMaxSizeJpeg(png(img)));
        assertEquals(200, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    void smallJpegIsKeptAsIs() throws Exception {
        BufferedImage img = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", bos);
        byte[] in = bos.toByteArray();
        assertArrayEquals(in, ImageCompressor.toMaxSizeJpeg(in));
    }

    @Test
    void bigJpegIsReencoded() throws Exception {
        BufferedImage img = new BufferedImage(1600, 2000, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(7);
        for (int y = 0; y < 2000; y++) for (int x = 0; x < 1600; x++) img.setRGB(x, y, r.nextInt(0xFFFFFF));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", bos);
        byte[] out = ImageCompressor.toMaxSizeJpeg(bos.toByteArray());
        assertTrue(out.length <= ImageCompressor.MAX_BYTES, "got " + out.length);
    }

    @Test
    void nonImageIsRejected() {
        assertThrows(BadRequestException.class, () -> ImageCompressor.toMaxSizeJpeg("not an image".getBytes()));
    }
}
