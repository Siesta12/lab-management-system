package com.nlt.common.security;

import com.nlt.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenService {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey secretKey;
    private final long expirationMs;

    public TokenService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BusinessException(500, "JWT密钥配置错误：security.jwt.secret 长度至少为32字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String userNo, List<String> roleCodes) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (!StringUtils.hasText(userNo)) {
            throw new BusinessException(400, "学号/工号不能为空");
        }

        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userNo", userNo)
                .claim("roleCodes", roleCodes == null ? Collections.emptyList() : roleCodes)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "无效的登录令牌");
        }

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(401, "无效或过期的登录令牌");
        }
    }

    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException(401, "登录令牌中的用户信息无效");
        }
    }

    public String parseUsername(String token) {
        Claims claims = parseClaims(token);
        Object userNo = claims.get("userNo");
        if (userNo == null) {
            userNo = claims.get("username");
        }
        return userNo == null ? null : String.valueOf(userNo);
    }

    @SuppressWarnings("unchecked")
    public List<String> parseRoleCodes(String token) {
        Claims claims = parseClaims(token);
        Object roleCodes = claims.get("roleCodes");

        if (roleCodes instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }

        if (roleCodes instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }

        return Collections.emptyList();
    }

    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization)) {
            throw new BusinessException(401, "缺少验证请求头");
        }

        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }

        return trimmed;
    }

    public Long getCurrentUserId(HttpServletRequest request) {
        return parseUserId(resolveToken(request));
    }

    public String getCurrentUsername(HttpServletRequest request) {
        return parseUsername(resolveToken(request));
    }

    public List<String> getCurrentRoleCodes(HttpServletRequest request) {
        return parseRoleCodes(resolveToken(request));
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
