package com.ctpop.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 관련 설정을 담당하는 클래스
 * 
 * 주요 기능:
 * - CORS(Cross-Origin Resource Sharing) 설정: 브라우저 보안 정책에 따른 도메인 간 요청 허용
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS 설정을 구성합니다.
     * 
     * 모든 출처, 메서드, 헤더에 대한 접근을 허용합니다.
     * 이는 개발 환경에서 API 테스트를 용이하게 하기 위함입니다.
     * 
     * @param registry CORS 설정 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
} 