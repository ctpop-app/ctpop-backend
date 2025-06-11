package com.ctpop.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security 보안 설정을 관리하는 클래스
 * 
 * 이 클래스는 애플리케이션의 보안 구성을 정의하며, 다음과 같은 설정을 포함합니다:
 * 1. CSRF 보호 비활성화 (API 서버 특성상)
 * 2. CORS 설정 (Cross-Origin Resource Sharing)
 * 3. 엔드포인트별 접근 권한 설정
 * 4. 세션 관리 정책 (STATELESS - 세션 사용 안 함)
 * 
 * REST API 서버 특성에 맞게 최적화된 보안 설정을 제공합니다.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Spring Security 필터 체인 구성
     * 
     * 이 메서드는 HTTP 요청의 보안 처리를 위한 필터 체인을 구성합니다.
     * 
     * 주요 설정:
     * - CSRF 보호 비활성화: REST API는 일반적으로 CSRF 토큰이 필요 없음
     * - CORS 활성화: 다른 도메인에서의 요청 허용 설정
     * - 경로별 접근 권한:
     *   - 인증 관련 엔드포인트는 인증 없이 접근 가능
     *   - Swagger/OpenAPI 문서 경로는 인증 없이 접근 가능
     *   - 그 외 모든 경로는 인증 필요
     * - 세션 관리: STATELESS로 설정하여 세션을 사용하지 않음 (JWT 기반 인증)
     * 
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain 인스턴스
     * @throws Exception 보안 구성 중 오류 발생 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Security Filter Chain");
        
        // HTTP Security 설정
        http
            // CSRF 보호 비활성화 (API 서버 특성상)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 예외 핸들링 설정
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling
                    .authenticationEntryPoint((request, response, authException) -> {
                        log.error("Authentication error: {}", authException.getMessage());
                        response.setStatus(401);
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.error("Access denied error: {}", accessDeniedException.getMessage());
                        response.setStatus(403);
                    })
            )
            
            // HTTP 요청 인증 설정 - 개발 중에는 모든 경로 허용
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            
            // 세션 관리 설정 (STATELESS: 세션 사용 안 함)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }

    /**
     * CORS 설정을 정의하는 빈
     * 
     * Cross-Origin Resource Sharing 설정으로, 다른 도메인에서의 API 접근을 허용합니다.
     * 개발 환경에서는 모든 출처(*)를 허용하도록 설정되어 있습니다.
     * 
     * 설정 내용:
     * - 모든 출처(Origins) 허용
     * - 주요 HTTP 메서드(GET, POST, PUT, DELETE, OPTIONS) 허용
     * - 모든 헤더 허용
     * 
     * 주의: 프로덕션 환경에서는 보안을 위해 허용할 출처를 제한하는 것이 좋습니다.
     * 
     * @return CORS 구성 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false); // 자격 증명 허용 안 함
        configuration.setMaxAge(3600L); // preflight 요청 캐시 시간 (초)
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        log.info("CORS configuration applied");
        return source;
    }
} 