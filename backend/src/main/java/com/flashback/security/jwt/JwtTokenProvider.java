package com.flashback.security.jwt;

import com.flashback.config.JwtProperties;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 生成与解析组件。
 *
 * 约定：Token 至少包含 userId 与 role 两个 claim，供统一鉴权层使用。
 */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpirationMillis());
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey())
                .compact();
    }

    public String createToken(AuthUser authUser) {
        return createToken(
                String.valueOf(authUser.getUserId()),
                Map.of(
                        CLAIM_USER_ID, authUser.getUserId(),
                        CLAIM_ROLE, authUser.getRole().name()));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public AuthUser parseAuthUser(String token) {
        Claims claims = parseClaims(token);
        Long userId = parseUserId(claims);
        String roleValue = String.valueOf(claims.get(CLAIM_ROLE));
        AuthRole role = AuthRole.from(roleValue);
        return new AuthUser(userId, role);
    }

    private Long parseUserId(Claims claims) {
        Object userIdValue = claims.get(CLAIM_USER_ID);
        if (userIdValue == null) {
            userIdValue = claims.getSubject();
        }
        if (userIdValue instanceof Long value) {
            return value;
        }
        if (userIdValue instanceof Integer value) {
            return value.longValue();
        }
        if (userIdValue instanceof String value) {
            return Long.parseLong(value);
        }
        throw new IllegalArgumentException("invalid userId claim");
    }

    private SecretKey secretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
