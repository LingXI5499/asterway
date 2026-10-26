package com.asterway.identity.captcha;

import com.asterway.identity.api.model.AccountModels.CaptchaChallenge;
import com.asterway.identity.infrastructure.security.SecureTokenGenerator;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;

// 图片和缺口都由服务端生成；页面只上报按原图换算后的位移。
@Component
public class CaptchaImageFactory {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 160;
    private static final int PIECE = 40;

    public Material create(UUID id, String purpose, Instant expiresAt) {
        int answerX = 60 + SecureTokenGenerator.randomInt(210);
        int top = 20 + SecureTokenGenerator.randomInt(100);
        BufferedImage background = background();
        BufferedImage piece = new BufferedImage(PIECE, PIECE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pieceGraphics = piece.createGraphics();
        try {
            pieceGraphics.setClip(new RoundRectangle2D.Double(0, 0, PIECE, PIECE, 12, 12));
            pieceGraphics.drawImage(background, -answerX, -top, null);
        } finally {
            pieceGraphics.dispose();
        }
        Graphics2D graphics = background.createGraphics();
        try {
            graphics.setColor(new Color(10, 25, 40, 175));
            graphics.fillRoundRect(answerX, top, PIECE, PIECE, 12, 12);
            graphics.setColor(Color.WHITE);
            graphics.drawRoundRect(answerX, top, PIECE, PIECE, 12, 12);
        } finally {
            graphics.dispose();
        }
        CaptchaChallenge challenge = new CaptchaChallenge(
                id, purpose, png(background), png(piece), WIDTH, HEIGHT, PIECE, PIECE, top, expiresAt
        );
        Material material = new Material(answerX, challenge, SecureTokenGenerator.token());
        return material;
    }

    private BufferedImage background() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setPaint(new GradientPaint(0, 0, color(), WIDTH, HEIGHT, color()));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            graphics.setStroke(new BasicStroke(3));
            for (int index = 0; index < 28; index++) {
                graphics.setColor(color());
                int x = SecureTokenGenerator.randomInt(WIDTH);
                int y = SecureTokenGenerator.randomInt(HEIGHT);
                int size = 15 + SecureTokenGenerator.randomInt(65);
                graphics.drawOval(x, y, size, size);
                graphics.drawLine(x, y, SecureTokenGenerator.randomInt(WIDTH), SecureTokenGenerator.randomInt(HEIGHT));
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private Color color() {
        Color color = new Color(
                40 + SecureTokenGenerator.randomInt(190),
                40 + SecureTokenGenerator.randomInt(190),
                40 + SecureTokenGenerator.randomInt(190)
        );
        return color;
    }

    private String png(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
            return dataUri;
        } catch (IOException exception) {
            throw new IllegalStateException("拼图图片生成失败", exception);
        }
    }

    public record Material(int answerX, CaptchaChallenge challenge, String verificationToken) {
    }
}
