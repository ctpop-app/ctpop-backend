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
        Date now = new Date();
        Date expiration = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 60 * 60 * 1000L);
        
        log.info("리프레시 토큰 생성 - 현재 시간: {}, 만료 시간: {}", now, expiration);
        
        String refreshToken = Jwts.builder()
            .setSubject(uuid)
            .claim("uuid", uuid)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecret())
            .compact();

        // Redis에 리프레시 토큰 저장
        String key = TOKEN_PREFIX + uuid + ":refresh";
        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRATION_DAYS, TimeUnit.DAYS);
        
        return refreshToken;
    }

    /**
     * 토큰을 검증하고 갱신합니다.
     */
    public TokenResponse validateToken(TokenRequest request) {
        String uuid = request.getUuid();
        String refreshToken = request.getRefreshToken();

        // Redis에서 리프레시 토큰 확인
        String storedToken = redisTemplate.opsForValue().get(TOKEN_PREFIX + uuid + ":refresh");
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new TokenException("유효하지 않은 토큰입니다.");
        }

        try {
            Claims claims = validateToken(refreshToken);
            if (claims.get("uuid", String.class).equals(uuid)) {
                // 이전 토큰 삭제
                redisTemplate.delete(TOKEN_PREFIX + uuid + ":refresh");
                // 새 토큰 발급
                String renewedRefreshToken = generateRefreshToken(uuid);
                return new TokenResponse(null, renewedRefreshToken, uuid);
            }
        } catch (TokenException e) {
            log.warn("리프레시 토큰 검증 실패: {}", e.getMessage());
        }
        throw new TokenException("유효하지 않은 토큰입니다.");
    }

    /**
     * OTP 인증을 위한 액세스 토큰을 검증합니다.
     */
    public String validateAccessToken(String accessToken) {
        try {
            Claims claims = validateToken(accessToken);
            return claims.get("uuid", String.class);
        } catch (TokenException e) {
            log.warn("액세스 토큰 검증 실패: {}", e.getMessage());
            throw new TokenException("유효하지 않은 액세스 토큰입니다.");
        }
    }

    /**
     * 로그아웃합니다.
     */
    public void logout(TokenRequest request) {
        String uuid = request.getUuid();
        String refreshToken = request.getRefreshToken();

        if (uuid == null || refreshToken == null) {
            throw new TokenException("유효하지 않은 로그아웃 요청입니다.");
        }

        // Redis에서 리프레시 토큰 제거
        redisTemplate.delete(TOKEN_PREFIX + uuid + ":refresh");
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