package com.ctpop.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 인증에 필요한 설정 정보를 관리하는 클래스
 * 
 * application.yml 파일의 app.jwt 프로퍼티를 자동으로 바인딩합니다.
 * JWT 토큰 생성 및 검증에 필요한 비밀키와 만료 시간 정보를 포함합니다.
 * 
 * 설정 정보:
 * - secret: JWT 토큰 서명에 사용되는 비밀키
 * - expirationMs: 액세스 토큰 만료 시간 (밀리초 단위)
 * - refreshExpirationMs: 리프레시 토큰 만료 시간 (밀리초 단위)
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    // JWT 토큰 서명에 사용되는 비밀키 (.env 파일의 JWT_SECRET을 사용)
    private String secret;
    
    // 액세스 토큰 만료 시간 (밀리초 단위, 기본값: 30분)
    private long expirationMs;
    
    // 리프레시 토큰 만료 시간 (밀리초 단위, 기본값: 30일)
    private long refreshExpirationMs;
} 