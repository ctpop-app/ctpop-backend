package com.ctpop.auth.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String phone;
    private String code;
} 