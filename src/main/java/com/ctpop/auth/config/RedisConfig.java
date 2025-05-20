package com.ctpop.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연동에 필요한 설정 정보를 관리하는 클래스
 * 
 * Redis는 OTP 상태 및 리프레시 토큰을 저장하는 데 사용되는 인메모리 데이터 스토어입니다.
 * 이 구성 클래스는 Redis와의 상호 작용을 위한 RedisTemplate 빈을 정의합니다.
 * 
 * 기능:
 * - 키와 값 모두 문자열로 직렬화하는 RedisTemplate 구성
 * - 이 템플릿은 OTP 코드와 리프레시 토큰 모두 문자열로 처리하기 위해 사용됨
 */
@Configuration
public class RedisConfig {
    
    /**
     * 문자열 키-값 쌍을 처리하는 RedisTemplate 빈을 생성합니다.
     * 
     * 이 템플릿은 다음과 같은 용도로 사용됩니다:
     * 1. OTP 상태 정보 저장 (otpService에서 사용)
     * 2. 리프레시 토큰 저장 (tokenService에서 사용)
     * 
     * 키와 값 모두 StringRedisSerializer를 사용하여 직렬화합니다.
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @return 구성된 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
} 