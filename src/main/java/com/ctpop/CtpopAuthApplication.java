package com.ctpop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * CT Pop 서비스의 인증 모듈 애플리케이션 진입점 클래스
 * 
 * 이 클래스는 Spring Boot 애플리케이션을 시작하는 메인 클래스입니다.
 * OTP 기반 인증 및 JWT 토큰 관리 기능을 제공하는 모듈입니다.
 * 
 * 주요 기능:
 * - 휴대폰 번호 기반 SMS OTP 인증
 * - JWT 액세스 토큰 및 리프레시 토큰 관리
 * - Redis를 사용한 OTP 및 토큰 상태 관리
 * 
 * 사용 기술:
 * - Spring Boot 3.4.5
 * - Spring Security
 * - Redis
 * - Twilio API (SMS 발송)
 * - JWT (인증 토큰)
 */
@SpringBootApplication
@EnableConfigurationProperties  // application.yml 속성 자동 바인딩 활성화
@EnableRedisRepositories        // Redis 저장소 기능 활성화
public class CtpopAuthApplication {
    /**
     * 애플리케이션의 메인 실행 메서드
     * 
     * @param args 명령행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(CtpopAuthApplication.class, args);
    }
} 