package com.seer.fitness.system.service;

import com.seer.fitness.system.config.CaptchaConfig;
import com.seer.fitness.system.dto.CaptchaConfigResponse;
import com.seer.fitness.system.dto.CaptchaResponse;
import com.seer.fitness.system.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 *
 * @author mocanjie
 */
@Service
@Slf4j
public class CaptchaService {

    @Autowired
    private CaptchaConfig captchaConfig;

    @Autowired
    private RedisUtil redisUtil;

    private final SecureRandom random = new SecureRandom();

    // 字体列表
    private static final String[] FONT_NAMES = {"Arial", "Times New Roman", "Courier New"};

    /**
     * 生成验证码
     *
     * @return 包含验证码ID和图片Base64的响应对象
     */
    public CaptchaResponse generateCaptcha() {
        if (!captchaConfig.isEnabled()) {
            return null;
        }

        // 生成验证码文本
        String code = generateRandomCode();

        // 生成验证码图片
        BufferedImage image = createCaptchaImage(code);

        // 转换为Base64
        String base64Image = imageToBase64(image);

        // 生成唯一ID
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 存储到Redis
        String redisKey = "captcha:" + captchaId;
        redisUtil.set(redisKey, code.toUpperCase(), captchaConfig.getExpireSeconds(), TimeUnit.SECONDS);

        log.info("生成验证码: id={}, code={}", captchaId, code);

        // 返回结果
        return new CaptchaResponse(
                captchaId,
                "data:image/png;base64," + base64Image,
                captchaConfig.getExpireSeconds()
        );
    }

    /**
     * 验证验证码
     * 注意：验证时不区分大小写，用户可以输入大写、小写或混合大小写
     *
     * @param captchaId 验证码ID
     * @param userInput 用户输入的验证码
     * @return 是否验证成功
     */
    public boolean verifyCaptcha(String captchaId, String userInput) {
        if (!captchaConfig.isEnabled()) {
            return true; // 如果禁用了验证码，直接返回true
        }

        if (captchaId == null || userInput == null) {
            return false;
        }

        String redisKey = "captcha:" + captchaId;
        String storedCode = redisUtil.get(redisKey, String.class);

        if (storedCode == null) {
            log.warn("验证码已过期或不存在: captchaId={}", captchaId);
            return false;
        }

        // 验证成功后删除验证码（防止重复使用）
        redisUtil.delete(redisKey);

        boolean isValid = storedCode.equalsIgnoreCase(userInput.trim());
        log.info("验证码校验结果: captchaId={}, userInput={}, storedCode={}, result={}",
                captchaId, userInput, storedCode, isValid);

        return isValid;
    }

    /**
     * 生成随机验证码文本
     */
    private String generateRandomCode() {
        String characters = captchaConfig.getType().getCharacters();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < captchaConfig.getLength(); i++) {
            int index = random.nextInt(characters.length());
            code.append(characters.charAt(index));
        }

        return code.toString();
    }

    /**
     * 创建验证码图片
     */
    private BufferedImage createCaptchaImage(String code) {
        int width = captchaConfig.getWidth();
        int height = captchaConfig.getHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置渲染质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        // 填充背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // 绘制干扰线
        drawInterferenceLines(g2d, width, height);

        // 绘制验证码文本
        drawCodeText(g2d, code, width, height);

        // 添加噪点
        drawNoise(g2d, width, height);

        g2d.dispose();
        return image;
    }

    /**
     * 绘制干扰线
     */
    private void drawInterferenceLines(Graphics2D g2d, int width, int height) {
        g2d.setStroke(new BasicStroke(2.0f));

        for (int i = 0; i < captchaConfig.getLineCount(); i++) {
            // 随机颜色
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));

            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);

            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 绘制验证码文本
     */
    private void drawCodeText(Graphics2D g2d, String code, int width, int height) {
        int codeLength = code.length();
        int charWidth = width / (codeLength + 1);

        for (int i = 0; i < codeLength; i++) {
            char c = code.charAt(i);

            // 随机字体
            String fontName = FONT_NAMES[random.nextInt(FONT_NAMES.length)];
            int fontSize = height / 2 + random.nextInt(height / 4);
            int fontStyle = random.nextBoolean() ? Font.BOLD : Font.PLAIN;
            Font font = new Font(fontName, fontStyle, fontSize);
            g2d.setFont(font);

            // 随机颜色（深色）
            g2d.setColor(new Color(random.nextInt(128), random.nextInt(128), random.nextInt(128)));

            // 计算字符位置
            int x = charWidth * (i + 1) - charWidth / 2;
            int y = height / 2 + fontSize / 3;

            // 随机旋转角度
            double angle = (random.nextDouble() - 0.5) * 0.5;
            g2d.rotate(angle, x, y);

            // 绘制字符
            g2d.drawString(String.valueOf(c), x - fontSize / 4, y);

            // 恢复旋转
            g2d.rotate(-angle, x, y);
        }
    }

    /**
     * 绘制噪点
     */
    private void drawNoise(Graphics2D g2d, int width, int height) {
        int noiseCount = width * height / 20;

        for (int i = 0; i < noiseCount; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            // 随机颜色
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g2d.fillOval(x, y, 1, 1);
        }
    }

    /**
     * 将图片转换为Base64编码
     */
    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("验证码图片转Base64失败", e);
            throw new RuntimeException("生成验证码失败", e);
        }
    }

    /**
     * 获取验证码配置信息
     */
    public CaptchaConfigResponse getCaptchaConfig() {
        return new CaptchaConfigResponse(
                captchaConfig.isEnabled(),
                captchaConfig.getType().name(),
                captchaConfig.getLength(),
                captchaConfig.getExpireSeconds()
        );
    }
}