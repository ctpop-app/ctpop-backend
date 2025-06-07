package com.ctpop.auth.dto.request;

import lombok.Data;

/**
 * 토큰 관련 요청을 처리하기 위한 DTO
 */
@Data
public class TokenRequest {
    private String uuid;          // 사용자 UUID
    private String accessToken;   // 액세스 토큰 (선택적)
    private String refreshToken;  // 리프레시 토큰 (선택적)
} 