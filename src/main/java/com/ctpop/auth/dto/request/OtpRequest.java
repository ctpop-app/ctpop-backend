package com.ctpop.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * OTP 관련 요청을 처리하기 위한 DTO
 */
@Getter
@Setter
public class OtpRequest {
    private String phone;
    private String code;
} 