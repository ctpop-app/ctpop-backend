package com.ctpop.auth.service;

import com.ctpop.auth.config.JwtConfig;
import com.ctpop.auth.dto.request.TokenRequest;
import com.ctpop.auth.dto.response.TokenResponse;
import com.ctpop.auth.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 토큰 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_PREFIX = "token:";
    private static final int ACCESS_TOKEN_EXPIRATION_MINUTES = 30;
    private static final int REFRESH_TOKEN_EXPIRATION_DAYS = 30;

    /**
     * 액세스 토큰을 생성합니다.
     */
    public String generateAccessToken(String uuid) {
        return Jwts.builder()
            .setSubject(uuid)
            .claim("uuid", uuid)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MINUTES * 60 * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecret())
            .compact();
    }

    /**
     * 리프레시 토큰을 생성합니다.
     */
    public String generateRefreshToken(String uuid) {
        return Jwts.builder()
            .setSubject(uuid)
            .claim("uuid", uuid)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 60 * 60 * 1000))
            .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecret())
            .compact();
    }

    /**
     * 토큰을 검증하고 갱신합니다.
     */
    public TokenResponse validateToken(TokenRequest request) {
        String uuid = request.getUuid();
        String refreshToken = request.getRefreshToken();

        // 리프레시 토큰이 있으면 검증 후 새 리프레시 토큰만 발급
        if (refreshToken != null) {
            try {
                Claims claims = validateToken(refreshToken);
                if (claims.get("uuid", String.class).equals(uuid)) {
                    // 리프레시 토큰의 유효기간을 30일로 갱신(새 토큰 발급)
                    String renewedRefreshToken = generateRefreshToken(uuid);
                    return new TokenResponse(null, renewedRefreshToken, uuid);
                }
            } catch (TokenException e) {
                log.warn("리프레시 토큰 검증 실패: {}", e.getMessage());
            }
        }
        throw new TokenException("유효하지 않은 토큰입니다.");
    }

    /**
     * 로그아웃합니다.
     */
    public void logout(TokenRequest request) {
        String uuid = request.getUuid();
        String accessToken = request.getAccessToken();
        String refreshToken = request.getRefreshToken();

        // Redis에서 토큰 제거
        if (accessToken != null) {
            redisTemplate.delete(TOKEN_PREFIX + uuid + ":access");
        }
        if (refreshToken != null) {
            redisTemplate.delete(TOKEN_PREFIX + uuid + ":refresh");
        }
    }

    /**
     * 토큰을 검증합니다.
     */
    private Claims validateToken(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(jwtConfig.getSecret())
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            throw new TokenException("토큰 검증에 실패했습니다: " + e.getMessage());
        }
    }
} 