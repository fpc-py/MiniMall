package com.mall.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 签发/校验单元测试（不依赖 Spring 容器与中间件）。
 */
class JwtUtilsTest {

    private static final String SECRET = "mall-unit-test-secret-key-0123456789abcdef";
    private static final long EXPIRATION = 3_600_000L;

    private final JwtUtils jwtUtils = new JwtUtils(SECRET, EXPIRATION);

    @Test
    @DisplayName("会员 Token：可解析出 userId/username/userType")
    void generateAndParseMemberToken() {
        String token = jwtUtils.generateToken(9527L, "m13800138000", UserType.MEMBER);

        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getUserId(token)).isEqualTo(9527L);
        assertThat(jwtUtils.getUsername(token)).isEqualTo("m13800138000");
        assertThat(jwtUtils.getUserType(token)).isEqualTo(UserType.MEMBER);

        LoginUser loginUser = jwtUtils.toLoginUser(jwtUtils.parseToken(token));
        assertThat(loginUser.userId()).isEqualTo(9527L);
        assertThat(loginUser.isMember()).isTrue();
        assertThat(loginUser.role()).isEqualTo("ROLE_MEMBER");
    }

    @Test
    @DisplayName("系统用户 Token：userType=sys，角色 ROLE_SYS")
    void generateAndParseSysToken() {
        String token = jwtUtils.generateToken(1L, "admin", UserType.SYS);

        assertThat(jwtUtils.getUserType(token)).isEqualTo(UserType.SYS);
        LoginUser loginUser = jwtUtils.toLoginUser(jwtUtils.parseToken(token));
        assertThat(loginUser.isSys()).isTrue();
        assertThat(loginUser.role()).isEqualTo("ROLE_SYS");
    }

    @Test
    @DisplayName("两参重载默认签发会员 Token")
    void twoArgOverloadDefaultsToMember() {
        String token = jwtUtils.generateToken(2L, "m13900139000");
        assertThat(jwtUtils.getUserType(token)).isEqualTo(UserType.MEMBER);
    }

    @Test
    @DisplayName("Token 只含必要声明，不泄漏手机号等敏感明文")
    void tokenCarriesNoSensitiveClaims() {
        String token = jwtUtils.generateToken(9527L, "m13800138000", UserType.MEMBER);
        Claims claims = jwtUtils.parseToken(token);

        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "username", "userType", "iat", "exp");
        assertThat(claims.get("phone")).isNull();
        assertThat(claims.get("password")).isNull();
    }

    @Test
    @DisplayName("被篡改的 Token 校验失败")
    void tamperedTokenIsRejected() {
        String token = jwtUtils.generateToken(9527L, "m13800138000", UserType.MEMBER);
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtUtils.validateToken(tampered)).isFalse();
        assertThatThrownBy(() -> jwtUtils.parseToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("其它密钥签发的 Token 校验失败")
    void tokenSignedWithOtherSecretIsRejected() {
        String token = new JwtUtils("another-secret-key-0123456789abcdefghijklmn", EXPIRATION)
                .generateToken(9527L, "m13800138000", UserType.MEMBER);

        assertThat(jwtUtils.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("过期 Token 校验失败并抛 ExpiredJwtException")
    void expiredTokenIsRejected() {
        JwtUtils expiredJwtUtils = new JwtUtils(SECRET, -1_000L);
        String token = expiredJwtUtils.generateToken(9527L, "m13800138000", UserType.MEMBER);

        assertThat(expiredJwtUtils.validateToken(token)).isFalse();
        assertThatThrownBy(() -> expiredJwtUtils.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("空/非法字符串不会抛出未捕获异常")
    void illegalTokenIsHandled() {
        assertThat(jwtUtils.validateToken(null)).isFalse();
        assertThat(jwtUtils.validateToken("")).isFalse();
        assertThat(jwtUtils.validateToken("not-a-jwt")).isFalse();
    }
}
