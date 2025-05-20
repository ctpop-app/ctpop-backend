package com.ctpop.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String phone;
    private String refreshToken;
} 