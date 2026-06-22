package io.github.canjiemo.momo.ai.config;

import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AI 敏感配置（API Key）可逆加密工具。
 * <p>
 * 采用 AES-256/GCM/NoPadding，密钥由配置项 {@code momo.ai.secret-key} 经 SHA-256 派生。
 * 加密结果以 {@code enc:} 前缀标记，便于与历史明文/脱敏占位值区分，兼容存量未加密数据。
 *
 * @author canjiemo@gmail.com
 */
@Slf4j
@Component
public class AiSecretCipher {

    /** 密文前缀标记，用于区分密文 / 明文 / 脱敏占位 */
    private static final String ENC_PREFIX = "enc:";
    /** 列表回显脱敏前缀 */
    private static final String MASK_PREFIX = "****";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom random = new SecureRandom();

    public AiSecretCipher(@Value("${momo.ai.secret-key:}") String configuredKey) {
        String key = configuredKey;
        if (key == null || key.isBlank()) {
            key = "momo-ai-default-dev-secret-please-change-in-production";
            log.warn("未配置 momo.ai.secret-key，已使用内置默认密钥。生产环境必须配置该项，否则 API Key 加密形同虚设！");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化 AI 加密密钥失败", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }

    public boolean isMasked(String value) {
        return value != null && value.startsWith(MASK_PREFIX);
    }

    /** 加密明文；为空、已加密或为脱敏占位值时原样返回（幂等，避免重复加密） */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank() || isEncrypted(plain) || isMasked(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv).put(cipherText).array();
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            throw new BusinessException("API Key 加密失败");
        }
    }

    /** 解密；非密文（历史明文）原样返回 */
    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(ENC_PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("API Key 解密失败，请检查 momo.ai.secret-key 是否被变更", e);
            throw new BusinessException("API Key 解密失败，请检查加密密钥配置是否变更");
        }
    }

    /** 脱敏回显：仅保留末 4 位，其余以 **** 替代；解密失败时返回固定占位 */
    public String mask(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        String plain;
        try {
            plain = decrypt(stored);
        } catch (Exception e) {
            return MASK_PREFIX;
        }
        if (plain == null || plain.length() <= 4) {
            return MASK_PREFIX;
        }
        return MASK_PREFIX + plain.substring(plain.length() - 4);
    }
}
