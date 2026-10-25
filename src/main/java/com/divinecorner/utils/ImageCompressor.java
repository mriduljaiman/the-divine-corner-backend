package com.divinecorner.utils;

import com.divinecorner.exception.BadRequestException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Re-encodes any uploaded picture as a JPEG of at most {@link #MAX_BYTES} bytes.
 * Quality is lowered first; if that is not enough the image is scaled down step by step,
 * so the loop always ends (a tiny JPEG is far below the limit).
 */
public final class ImageCompressor {

    public static final int MAX_BYTES = 20 * 1024;
    private static final int MAX_W = 800;
    private static final int MAX_H = 1067;
    private static final int MIN_SIDE = 120;
    private static final float[] QUALITIES = {0.85f, 0.78f, 0.7f, 0.62f, 0.55f, 0.48f, 0.42f, 0.36f};

    private ImageCompressor() {
    }

    public static byte[] toMaxSizeJpeg(byte[] input) {
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(input));
        } catch (IOException e) {
            src = null;
        }
        if (src == null) {
            throw new BadRequestException("Unsupported image. Please upload a JPG or PNG photo.");
        }

        // Already a small JPEG of the right size (the admin app compresses in the browser):
        // keep it as-is instead of re-encoding and losing quality a second time.
        boolean jpeg = input.length > 3 && (input[0] & 0xFF) == 0xFF && (input[1] & 0xFF) == 0xD8;
        if (jpeg && input.length <= MAX_BYTES && src.getWidth() <= MAX_W && src.getHeight() <= MAX_H) {
            return input;
        }

        double scale = Math.min(1.0, Math.min((double) MAX_W / src.getWidth(), (double) MAX_H / src.getHeight()));
        while (true) {
            int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
            BufferedImage frame = flatten(src, w, h);
            for (float q : QUALITIES) {
                byte[] out = encode(frame, q);
                if (out.length <= MAX_BYTES) {
                    return out;
                }
            }
            if (Math.min(w, h) <= MIN_SIDE) {
                // practically unreachable: a 120px JPEG at q=0.36 is a few KB
                byte[] out = encode(frame, 0.25f);
                if (out.length <= MAX_BYTES) return out;
                throw new BadRequestException("Could not compress image under 20KB");
            }
            scale *= 0.85;
        }
    }

    /** Resize onto a white background (JPEG has no transparency). */
    private static BufferedImage flatten(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static byte[] encode(BufferedImage img, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam p = writer.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(quality);
            writer.write(null, new IIOImage(img, null, null), p);
            ios.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("JPEG encoding failed", e);
        } finally {
            writer.dispose();
        }
    }
}
