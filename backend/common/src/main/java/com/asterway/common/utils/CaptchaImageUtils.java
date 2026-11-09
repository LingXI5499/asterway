/*
 * 集中处理Captcha图片Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入Color，下面可以直接使用这个类型或静态成员。
import java.awt.Color;
// 引入Graphics2D，下面可以直接使用这个类型或静态成员。
import java.awt.Graphics2D;
// 引入RenderingHints，下面可以直接使用这个类型或静态成员。
import java.awt.RenderingHints;
// 引入Area，下面可以直接使用这个类型或静态成员。
import java.awt.geom.Area;
// 引入Ellipse2D，下面可以直接使用这个类型或静态成员。
import java.awt.geom.Ellipse2D;
// 引入Rectangle2D，下面可以直接使用这个类型或静态成员。
import java.awt.geom.Rectangle2D;
// 引入Buffered图片（BufferedImage），下面可以直接使用这个类型或静态成员。
import java.awt.image.BufferedImage;
// 引入ByteArray输出分段回复流（ByteArrayOutputStream），下面可以直接使用这个类型或静态成员。
import java.io.ByteArrayOutputStream;
// 引入IOException，下面可以直接使用这个类型或静态成员。
import java.io.IOException;
// 引入SecureRandom，下面可以直接使用这个类型或静态成员。
import java.security.SecureRandom;
// 引入基础64（Base64），下面可以直接使用这个类型或静态成员。
import java.util.Base64;
// 引入图片IO（ImageIO），下面可以直接使用这个类型或静态成员。
import javax.imageio.ImageIO;

/* 生成本地拼图图像，不依赖外部图片服务；答案只交给服务端挑战存储。 */
// 定义类 CaptchaImageUtils；集中处理Captcha图片Utils的重复规则，避免各业务模块分别实现。
public final class CaptchaImageUtils {

    // 准备RANDOM，类型为 SecureRandom；初始内容是新建 SecureRandom 实例。
    private static final SecureRandom RANDOM = new SecureRandom();

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private CaptchaImageUtils() {
    }

    /* 服务端内部图像结果，不直接返回给客户端。 */
    // 定义保存一组固定字段的 record PuzzleData；集中处理Captcha图片Utils的重复规则，避免各业务模块分别实现。
    public record PuzzleData(
            // 后台（background），使用 String 类型保存；可为 null，表示尚未提供。
            String background,
            // piece，使用 String 类型保存；可为 null，表示尚未提供。
            String piece,
            // top，使用 int 类型保存。
            int top,
            // answer，使用 int 类型保存。
            int answer
    ) {
    }

    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    public static PuzzleData create() {
        // 准备后台（background），类型为 BufferedImage；初始内容是新建 Buffered图片（BufferedImage） 实例。
        BufferedImage background = new BufferedImage(320, 160, BufferedImage.TYPE_INT_RGB);
        // 准备canvas，类型为 Graphics2D；初始内容是创建 Graphics。
        Graphics2D canvas = background.createGraphics();
        // 设置 RenderingHint 的值。
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 设置 Color 的值。
        canvas.setColor(new Color(214, 229, 238));
        // 调用 canvas.fillRect 完成填充Rect处理。
        canvas.fillRect(0, 0, 320, 160);
        // 准备索引（index），类型为 int；初始内容是固定数值 0。
        // 逐次执行循环：for (int index = 0; index < 80; index++)；每轮处理一条数据或推进一次检查。
        // 完成本次状态更新或调用：index++;。
        for (int index = 0; index < 80; index++) {
            // 设置 Color 的值。
            canvas.setColor(new Color(40 + RANDOM.nextInt(170), 40 + RANDOM.nextInt(170), 40 + RANDOM.nextInt(170)));
            // 调用 canvas.fillOval 完成填充Oval处理。
            canvas.fillOval(RANDOM.nextInt(320), RANDOM.nextInt(160), 12 + RANDOM.nextInt(60), 12 + RANDOM.nextInt(60));
        }
        // 准备left，类型为 int；初始内容是调用 70 + RANDOM.nextInt 完成下一个Int处理。
        int left = 70 + RANDOM.nextInt(170);
        // 准备top，类型为 int；初始内容是调用 30 + RANDOM.nextInt 完成下一个Int处理。
        int top = 30 + RANDOM.nextInt(60);
        // 准备shape，类型为 Area；初始内容是新建 Area 实例。
        Area shape = new Area(new Rectangle2D.Double(10, 10, 40, 40));
        // 加入集合或登记一条待保存记录。
        shape.add(new Area(new Ellipse2D.Double(23, 3, 14, 14)));
        // 调用 shape.subtract 完成subtract处理。
        shape.subtract(new Area(new Ellipse2D.Double(43, 23, 14, 14)));
        // 准备piece，类型为 BufferedImage；初始内容是新建 Buffered图片（BufferedImage） 实例。
        BufferedImage piece = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        // 准备片段（fragment），类型为 Graphics2D；初始内容是创建 Graphics。
        Graphics2D fragment = piece.createGraphics();
        // 设置 Clip 的值。
        fragment.setClip(shape);
        // 调用 fragment.drawImage 完成draw图片处理。
        fragment.drawImage(background, -left, -top, null);
        // 关闭连接池并释放数据库连接。
        fragment.dispose();
        // 调用 canvas.translate 完成translate处理。
        canvas.translate(left, top);
        // 设置 Color 的值。
        canvas.setColor(new Color(255, 255, 255, 190));
        // 把已校验的请求字段填入待保存的记录。
        canvas.fill(shape);
        // 设置 Color 的值。
        canvas.setColor(new Color(36, 60, 82));
        // 调用 canvas.draw 完成draw处理。
        canvas.draw(shape);
        // 关闭连接池并释放数据库连接。
        canvas.dispose();
        // 把新建 Puzzle数据（PuzzleData） 实例交还给调用方，本方法到此结束。
        return new PuzzleData(encode(background), encode(piece), top, left);
    }

    // encode方法：把原始密码编码成不可直接还原的密码摘要；参数负责传入本次处理所需的数据。
    // 图片（image），使用 BufferedImage 类型保存。
    private static String encode(BufferedImage image) {
        // 准备输出（output），类型为 ByteArrayOutputStream；初始内容是新建 ByteArray输出分段回复流（ByteArrayOutputStream） 实例。
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // 把内容写入当前输出或响应。
            ImageIO.write(image, "png", output);
            // 把读取 Encoder 的值交还给调用方，本方法到此结束。
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("验证码图片生成失败");
        }
    }
}
