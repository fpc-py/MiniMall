package com.mall.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验。
 * 约定：subject = 用户ID，claim username = 用户名，claim userType = 主体类型（member/sys）。
 * Token 中不放手机号、密码等敏感明文。
 */
@Component
public class JwtUtils {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_USER_TYPE = "userType";

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtils(@Value("${mall.jwt.secret}") String secret,
                    @Value("${mall.jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /** 会员 Token */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, UserType.MEMBER);
    }

    public String generateToken(Long userId, String username, UserType userType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_USER_TYPE, (userType == null ? UserType.MEMBER : userType).getCode())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /** 解析 Token；签名错误/过期会抛 JwtException，由调用方处理 */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public String getUsername(String token) {
        return parseToken(token).get(CLAIM_USERNAME, String.class);
    }

    /** 解析主体类型，缺省按会员处理（兼容历史 Token） */
    public UserType getUserType(String token) {
        UserType type = UserType.of(parseToken(token).get(CLAIM_USER_TYPE, String.class));
        return type == null ? UserType.MEMBER : type;
    }

    /** 由 Claims 直接构造登录主体，避免重复解析 */
    public LoginUser toLoginUser(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        UserType type = UserType.of(claims.get(CLAIM_USER_TYPE, String.class));
        return new LoginUser(userId, username, type == null ? UserType.MEMBER : type);
    }

    /** Token 有效期（毫秒） */
    public long getExpiration() {
        return expiration;
    }
}
