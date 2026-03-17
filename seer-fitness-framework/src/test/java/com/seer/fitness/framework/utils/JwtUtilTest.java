package com.seer.fitness.framework.utils;

import com.seer.fitness.framework.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * JWT 工具类单元测试
 * <p>
 * Mock JwtConfig，测试 token 的生成/解析/字段提取/过期检测等逻辑。
 * LENIENT：部分测试（如 parseToken 格式校验）不需要生成 token，@BeforeEach stub 不参与。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtUtilTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String SECRET = "9F922B9833DEDAC0176412709F157797"; // 32字节，256位
    private static final long EXPIRATION_24H = 86_400_000L;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(SECRET);
        when(jwtConfig.getExpiration()).thenReturn(EXPIRATION_24H);
    }

    // -----------------------------------------------------------------------
    // 平台管理员 Token（无租户信息）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("生成平台管理员 token → 可解析出用户名和 userId")
    void generateToken_adminCanParse() {
        String token = jwtUtil.generateToken("admin", 1L);

        assertEquals("admin", jwtUtil.getUsernameFromToken(token));
        assertEquals(1L, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("平台管理员 token → 不含租户信息")
    void generateToken_adminHasNoTenantInfo() {
        String token = jwtUtil.generateToken("admin", 1L);

        assertFalse(jwtUtil.hasTenantInfo(token));
        assertNull(jwtUtil.getTenantIdFromToken(token));
        assertNull(jwtUtil.getTenantCodeFromToken(token));
    }

    @Test
    @DisplayName("平台管理员 token → tokenId 不为空")
    void generateToken_hasTokenId() {
        String token = jwtUtil.generateToken("admin", 1L);

        assertNotNull(jwtUtil.getTokenIdFromToken(token));
        assertFalse(jwtUtil.getTokenIdFromToken(token).isBlank());
    }

    // -----------------------------------------------------------------------
    // 租户用户 Token（含租户信息）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("生成租户 token → 可解析出租户 ID 和租户编码")
    void generateTokenWithTenant_hasTenantInfo() {
        String token = jwtUtil.generateTokenWithTenant("teacher01", 100L, 10L, "SCHOOL_A");

        assertTrue(jwtUtil.hasTenantInfo(token));
        assertEquals(10L, jwtUtil.getTenantIdFromToken(token));
        assertEquals("SCHOOL_A", jwtUtil.getTenantCodeFromToken(token));
    }

    @Test
    @DisplayName("租户 token → 基本字段也正确")
    void generateTokenWithTenant_basicFields() {
        String token = jwtUtil.generateTokenWithTenant("teacher01", 100L, 10L, "SCHOOL_A");

        assertEquals("teacher01", jwtUtil.getUsernameFromToken(token));
        assertEquals(100L, jwtUtil.getUserIdFromToken(token));
    }

    // -----------------------------------------------------------------------
    // 两次生成 → tokenId 唯一
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("同一用户两次生成 token → tokenId 不同")
    void generateToken_uniqueTokenIds() {
        String t1 = jwtUtil.generateToken("admin", 1L);
        String t2 = jwtUtil.generateToken("admin", 1L);

        assertNotEquals(jwtUtil.getTokenIdFromToken(t1), jwtUtil.getTokenIdFromToken(t2));
    }

    // -----------------------------------------------------------------------
    // 格式/签名校验
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("合法 token → validateTokenFormat 返回 true")
    void validateFormat_valid() {
        String token = jwtUtil.generateToken("admin", 1L);
        assertTrue(jwtUtil.validateTokenFormat(token));
    }

    @Test
    @DisplayName("篡改 token → validateTokenFormat 返回 false")
    void validateFormat_tampered() {
        String token = jwtUtil.generateToken("admin", 1L);
        // 替换最后几个字符使签名失效
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertFalse(jwtUtil.validateTokenFormat(tampered));
    }

    @Test
    @DisplayName("垃圾字符串 → parseToken 抛出 RuntimeException")
    void parseToken_invalidThrows() {
        assertThrows(RuntimeException.class, () -> jwtUtil.parseToken("not.a.jwt.token"));
    }

    // -----------------------------------------------------------------------
    // 过期 Token
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("已过期的 token -> parseToken 抛出 RuntimeException（消息含过期提示）")
    void parseToken_expiredThrows() {
        // 使用极短过期时间生成 token，然后立即解析
        when(jwtConfig.getExpiration()).thenReturn(-1000L); // 负数 → 已过期
        String expiredToken = jwtUtil.generateToken("admin", 1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jwtUtil.parseToken(expiredToken));
        assertTrue(ex.getMessage().contains("过期") || ex.getMessage().contains("Token"),
                "消息应说明 token 已过期，实际: " + ex.getMessage());
    }
}
