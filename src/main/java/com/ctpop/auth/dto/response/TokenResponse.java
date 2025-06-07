package com.ctpop.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 토큰 관련 응답을 처리하기 위한 DTO
 *
 * accessToken은 OTP 인증 직후에만 사용, 이후 인증은 refreshToken만 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken; // OTP 인증 직후에만 사용, 일반 인증에서는 null
    private String refreshToken;
    private String uuid;
} 