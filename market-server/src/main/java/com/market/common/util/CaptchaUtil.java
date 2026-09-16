package com.market.common.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 图形验证码生成工具（无需第三方依赖，基于 JDK AWT）。
 */
public final class CaptchaUtil {

    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;

    private CaptchaUtil() {
    }

    public static String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    public static String randomNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String toBase64Png(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(150, 220));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
            // 噪点
            for (int i = 0; i < 40; i++) {
                g.setColor(randomColor(150, 220));
                g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
            }

            int charWidth = WIDTH / (code.length() + 1);
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor(20, 120));
                g.setFont(new Font("SansSerif", Font.BOLD, 28 + RANDOM.nextInt(5)));
                double angle = (RANDOM.nextDouble() - 0.5) * 0.6;
                int x = charWidth / 2 + i * charWidth + RANDOM.nextInt(6);
                int y = 32 + RANDOM.nextInt(6);
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    private static Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + RANDOM.nextInt(bound), min + RANDOM.nextInt(bound), min + RANDOM.nextInt(bound));
    }
}
