package com.seer.fitness.system.utils;

import com.seer.fitness.system.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类
 *
 * @author seer-fitness
 */
@Component
@Slf4j
public class JwtUtil {

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 生成JWT Token（不含租户信息，用于平台管理员）
     */
    public String generateToken(String username, Long userId) {
        return generateTokenWithTenant(username, userId, null, null, null);
    }

    /**
     * 生成JWT Token（包含租户信息）
     * 阶段5新增：支持多租户隔离
     *
     * @param username   用户名
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @param tenantCode 租户编码
     * @param schemaName Schema名称
     * @return JWT Token
     */
    public String generateTokenWithTenant(String username, Long userId,
                                         Long tenantId, String tenantCode, String schemaName) {
        String tokenId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());

        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        // 构建JWT
        var builder = Jwts.builder()
                .setId(tokenId)
                .setSubject(username)
                .claim("userId", userId)
                .claim("tokenId", tokenId)
                .setIssuedAt(now)
                .setExpiration(expiration);

        // 如果有租户信息，添加到JWT中
        if (tenantId != null && tenantCode != null && schemaName != null) {
            builder.claim("tenantId", tenantId);
            builder.claim("tenantCode", tenantCode);
            builder.claim("schemaName", schemaName);
        }

        return builder.signWith(key).compact();
    }

    /**
     * 解析JWT Token
     */
    public Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期");
        } catch (JwtException e) {
            log.warn("JWT Token解析失败: {}", e.getMessage());
            throw new RuntimeException("Token无效");
        }
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取TokenId
     */
    public String getTokenIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenId", String.class);
    }

    /**
     * 从Token中获取租户ID
     * 阶段5新增
     */
    public Long getTenantIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("tenantId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取租户编码
     * 阶段5新增
     */
    public String getTenantCodeFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("tenantCode", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取Schema名称
     * 阶段5新增
     */
    public String getSchemaNameFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("schemaName", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查Token是否包含租户信息
     * 阶段5新增
     */
    public boolean hasTenantInfo(String token) {
        return getTenantIdFromToken(token) != null;
    }

    /**
     * 验证Token是否有效（仅验证格式和签名，不验证过期时间）
     */
    public boolean validateTokenFormat(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}