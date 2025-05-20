package com.ctpop.auth.service;

import com.ctpop.auth.config.JwtConfig;
import com.ctpop.auth.exception.OtpException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 관련 비즈니스 로직을 처리하는 서비스
 * 
 * 이 서비스는 다음과 같은 기능을 제공합니다:
 * 1. JWT 액세스 토큰 생성 - 클라이언트 인증에 사용되는 짧은 수명의 토큰
 * 2. JWT 리프레시 토큰 생성 - 액세스 토큰 갱신에 사용되는 긴 수명의 토큰
 * 3. 리프레시 토큰을 이용한 액세스 토큰 갱신
 * 4. 로그아웃 시 토큰 무효화
 * 
 * 모든 리프레시 토큰은 Redis에 저장되어 관리됩니다.
 */
@Service
@RequiredArgsConstructor
public class TokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;

    // Redis에 저장할 토큰 관련 키의 접두사
    private static final String TOKEN_PREFIX = "token:";

    /**
     * 액세스 토큰을 생성합니다.
     * 액세스 토큰은 클라이언트가 API에 접근할 때 사용하는 인증 토큰으로,
     * 상대적으로 짧은 유효 기간을 가집니다.
     * 
     * @param phone 사용자 전화번호 (토큰의 subject로 사용)
     * @return 생성된 JWT 액세스 토큰
     */
    public String generateAccessToken(String phone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
            .setSubject(phone)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecret())
            .compact();
    }

    /**
     * 리프레시 토큰을 생성하고 Redis에 저장합니다.
     * 리프레시 토큰은 액세스 토큰이 만료되었을 때 새로운 액세스 토큰을 발급받기 위해 사용되며,
     * 액세스 토큰보다 긴 유효 기간을 가집니다.
     * 
     * @param phone 사용자 전화번호 (토큰의 subject로 사용)
     * @return 생성된 JWT 리프레시 토큰
     */
    public String generateRefreshToken(String phone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshExpirationMs());

        String refreshToken = Jwts.builder()
            .setSubject(phone)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecret())
            .compact();

        // Redis에 리프레시 토큰 저장
        // 키: "token:{전화번호}", 값: 리프레시 토큰, 만료 시간: 설정된 리프레시 토큰 만료 시간
        String key = TOKEN_PREFIX + phone;
        redisTemplate.opsForValue().set(
            key, 
            refreshToken,
            jwtConfig.getRefreshExpirationMs(), 
            TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    /**
     * 리프레시 토큰을 검증하고 새로운 액세스 토큰을 발급합니다.
     * 
     * 검증 과정은 다음과 같습니다:
     * 1. Redis에 저장된 리프레시 토큰과 요청된 리프레시 토큰 비교
     * 2. 토큰의 서명 검증
     * 3. 토큰에 포함된 subject(전화번호)와 요청된 전화번호 비교
     * 
     * 모든 검증이 성공하면 새로운 액세스 토큰을 발급합니다.
     * 
     * @param phone 사용자 전화번호
     * @param refreshToken 검증할 리프레시 토큰
     * @return 새로운 액세스 토큰, 검증 실패 시 null
     */
    public String refreshAccessToken(String phone, String refreshToken) {
        // Redis에서 저장된 리프레시 토큰 조회
        String key = TOKEN_PREFIX + phone;
        String storedToken = redisTemplate.opsForValue().get(key);

        // 토큰 일치 여부 확인
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            return null;
        }

        try {
            // 토큰 유효성 검증
            Claims claims = Jwts.parser()
                .setSigningKey(jwtConfig.getSecret())
                .parseClaimsJws(refreshToken)
                .getBody();

            // 전화번호 일치 여부 확인
            if (!claims.getSubject().equals(phone)) {
                return null;
            }

            // 새로운 액세스 토큰 발급
            return generateAccessToken(phone);
        } catch (Exception e) {
            // 토큰 검증 실패 (만료, 변조 등)
            return null;
        }
    }

    /**
     * 로그아웃 시 Redis에서 리프레시 토큰을 제거합니다.
     * 이렇게 하면 리프레시 토큰이 무효화되어 더 이상 새로운 액세스 토큰을 발급받을 수 없게 됩니다.
     * 
     * @param phone 사용자 전화번호
     */
    public void removeRefreshToken(String phone) {
        String key = TOKEN_PREFIX + phone;
        redisTemplate.delete(key);
    }
} 