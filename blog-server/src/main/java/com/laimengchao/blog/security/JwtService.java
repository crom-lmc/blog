package com.laimengchao.blog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 博客自有 JWT（HS256）。密钥只从环境变量 BLOG_JWT_SECRET 读取，
 * 不落库、不进代码库。权限不写进令牌：每次请求现查库，撤权即时生效。
 */
@Slf4j
@Component
public class JwtService {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtService(@Value("${blog.jwt.secret}") String secret,
                      @Value("${blog.jwt.expire-seconds:86400}") long expireSeconds) {
        byte[] bytes = isBase64(secret) ? Decoders.BASE64.decode(secret)
                : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("BLOG_JWT_SECRET 至少需要 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireSeconds = expireSeconds;
    }

    public String issue(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    /** 解析成功返回用户 ID；令牌无效或过期返回 null，由调用方决定怎么拒绝。 */
    public Long parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.debug("JWT 解析失败：{}", e.getMessage());
            return null;
        }
    }

    private static boolean isBase64(String s) {
        if (s == null || s.length() % 4 != 0) return false;
        return s.matches("[A-Za-z0-9+/=]+");
    }
}
