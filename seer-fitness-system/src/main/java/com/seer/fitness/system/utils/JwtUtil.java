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
     * 生成JWT Token
     */
    public String generateToken(String username, Long userId) {
        String tokenId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());

        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setId(tokenId)
                .setSubject(username)
                .claim("userId", userId)
                .claim("tokenId", tokenId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key)
                .compact();
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