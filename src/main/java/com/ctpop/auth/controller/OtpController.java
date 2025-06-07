package com.ctpop.auth.controller;

import com.ctpop.auth.dto.request.OtpRequest;
import com.ctpop.auth.dto.response.TokenResponse;
import com.ctpop.auth.service.OtpService;
import com.ctpop.auth.exception.OtpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP 인증 관련 API를 제공하는 컨트롤러
 * 
 * 이 컨트롤러는 다음과 같은 엔드포인트를 제공합니다:
 * 1. OTP 전송 - 사용자 전화번호로 인증 코드 전송
 * 2. OTP 검증 - 사용자가 입력한 코드 검증 및 토큰 발급
 */
@Slf4j
@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;
    
    /**
     * 인증번호를 발송합니다.
     */
    @PostMapping("/send")
    public ResponseEntity<TokenResponse> sendOtp(@RequestBody OtpRequest request) {
        log.info("OTP 전송 요청 수신 - 전화번호: {}", request.getPhone());
        TokenResponse response = otpService.sendOtp(request.getPhone());
        log.info("OTP 전송 완료 - UUID: {}", response.getUuid());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 인증번호를 확인하고 토큰을 발급합니다.
     */
    @PostMapping("/verify")
    public ResponseEntity<TokenResponse> verifyOtp(
            @RequestBody OtpRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("OTP 검증 요청 수신 - 전화번호: {}, 코드: {}", request.getPhone(), request.getCode());
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.error("유효하지 않은 인증 정보 - Authorization 헤더: {}", authorization);
            throw new OtpException("유효하지 않은 인증 정보입니다.");
        }
        
        // Bearer 토큰에서 액세스 토큰 추출
        String accessToken = authorization.replace("Bearer ", "");
        log.info("액세스 토큰 추출 완료");
        
        TokenResponse response = otpService.verifyOtp(request.getPhone(), request.getCode(), accessToken);
        log.info("OTP 검증 완료 - UUID: {}", response.getUuid());
        return ResponseEntity.ok(response);
    }
} 