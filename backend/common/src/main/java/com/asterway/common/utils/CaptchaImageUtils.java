package com.asterway.common.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.imageio.ImageIO;

/** 生成本地拼图图像，不依赖外部图片服务；答案只交给服务端挑战存储。 */
public final class CaptchaImageUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaImageUtils() {
    }

    /** 服务端内部图像结果，不直接返回给客户端。 */
    public record PuzzleData(
            String background,
            String piece,
            int top,
            int answer
    ) {
    }

    public static PuzzleData create() {
        BufferedImage background = new BufferedImage(320, 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = background.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(new Color(214, 229, 238));
        canvas.fillRect(0, 0, 320, 160);
        for (int index = 0; index < 80; index++) {
            canvas.setColor(new Color(40 + RANDOM.nextInt(170), 40 + RANDOM.nextInt(170), 40 + RANDOM.nextInt(170)));
            canvas.fillOval(RANDOM.nextInt(320), RANDOM.nextInt(160), 12 + RANDOM.nextInt(60), 12 + RANDOM.nextInt(60));
        }
        int left = 70 + RANDOM.nextInt(170);
        int top = 30 + RANDOM.nextInt(60);
        Area shape = new Area(new Rectangle2D.Double(10, 10, 40, 40));
        shape.add(new Area(new Ellipse2D.Double(23, 3, 14, 14)));
        shape.subtract(new Area(new Ellipse2D.Double(43, 23, 14, 14)));
        BufferedImage piece = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fragment = piece.createGraphics();
        fragment.setClip(shape);
        fragment.drawImage(background, -left, -top, null);
        fragment.dispose();
        canvas.translate(left, top);
        canvas.setColor(new Color(255, 255, 255, 190));
        canvas.fill(shape);
        canvas.setColor(new Color(36, 60, 82));
        canvas.draw(shape);
        canvas.dispose();
        return new PuzzleData(encode(background), encode(piece), top, left);
    }

    private static String encode(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException error) {
            throw new IllegalStateException("验证码图片生成失败");
        }
    }
}
