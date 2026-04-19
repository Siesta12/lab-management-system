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
            throw new IllegalArgumentException("security.jwt.secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成登录令牌
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param roleCodes 角色编码列表
     * @return JWT
     */
    public String generateToken(Long userId, String username, List<String> roleCodes) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCodes", roleCodes == null ? Collections.emptyList() : roleCodes)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token，返回全部 Claims
     *
     * @param token JWT
     * @return Claims
     */
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
            throw new BusinessException(401, "无效或已过期的登录令牌");
        }
    }

    /**
     * 从 token 中解析用户ID
     *
     * @param token JWT
     * @return 用户ID
     */
    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException(401, "登录令牌中的用户信息无效");
        }
    }

    /**
     * 从 token 中解析用户名
     *
     * @param token JWT
     * @return 用户名
     */
    public String parseUsername(String token) {
        Claims claims = parseClaims(token);
        Object username = claims.get("username");
        return username == null ? null : String.valueOf(username);
    }

    /**
     * 从 token 中解析角色编码列表
     *
     * @param token JWT
     * @return 角色编码列表
     */
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

    /**
     * 从请求头中提取 token
     *
     * @param request HTTP请求
     * @return token
     */
    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization)) {
            throw new BusinessException(401, "缺少认证请求头");
        }

        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }

        return trimmed;
    }

    /**
     * 从请求中获取当前登录用户ID
     *
     * @param request HTTP请求
     * @return 用户ID
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        return parseUserId(resolveToken(request));
    }

    /**
     * 从请求中获取当前登录用户名
     *
     * @param request HTTP请求
     * @return 用户名
     */
    public String getCurrentUsername(HttpServletRequest request) {
        return parseUsername(resolveToken(request));
    }

    /**
     * 从请求中获取当前登录用户角色
     *
     * @param request HTTP请求
     * @return 角色编码列表
     */
    public List<String> getCurrentRoleCodes(HttpServletRequest request) {
        return parseRoleCodes(resolveToken(request));
    }

    /**
     * 获取 token 过期时间（毫秒）
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}