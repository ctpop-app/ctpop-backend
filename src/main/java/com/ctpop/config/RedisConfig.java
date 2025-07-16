package com.ctpop.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연동에 필요한 설정 정보를 관리하는 클래스
 * 
 * Redis는 OTP 상태 및 리프레시 토큰을 저장하는 데 사용되는 인메모리 데이터 스토어입니다.
 * 이 구성 클래스는 Redis와의 상호 작용을 위한 RedisTemplate 빈을 정의합니다.
 * 
 * 기능:
 * - Object 타입을 지원하는 RedisTemplate 구성 (위치 정보 등)
 * - 이 템플릿은 OTP 코드와 리프레시 토큰 모두 문자열로 처리하기 위해 사용됨
 */
@Configuration
public class RedisConfig {
    
    /**
     * Object 타입을 지원하는 RedisTemplate 빈을 생성합니다.
     * 
     * 이 템플릿은 다음과 같은 용도로 사용됩니다:
     * 1. 사용자 위치 정보 저장 (ProfileService에서 사용)
     * 2. 거리 계산 결과 저장
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @return 구성된 RedisTemplate 인스턴스
     */
    @Bean("objectRedisTemplate")
    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        
        // Object 타입 정보를 포함한 JSON 직렬화
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        
        return template;
    }
} 