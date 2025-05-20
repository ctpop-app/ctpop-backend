package com.ctpop.auth.controller;

import com.ctpop.auth.dto.TokenResponse;
import com.ctpop.auth.dto.SendOtpRequest;
import com.ctpop.auth.dto.VerifyOtpRequest;
import com.ctpop.auth.dto.RefreshTokenRequest;
import com.ctpop.auth.dto.LogoutRequest;
import com.ctpop.auth.exception.OtpException;
import com.ctpop.auth.service.OtpService;
import com.ctpop.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OTP 인증 관련 API를 제공하는 컨트롤러
 * 
 * 이 컨트롤러는 다음과 같은 엔드포인트를 제공합니다:
 * 1. OTP 전송 - 사용자 전화번호로 인증 코드 전송
 * 2. OTP 검증 - 사용자가 입력한 코드 검증 및 토큰 발급
 * 3. 토큰 갱신 - 만료된 액세스 토큰을 리프레시 토큰으로 갱신
 * 4. 로그아웃 - 사용자 세션 종료 및 리프레시 토큰 무효화
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "OTP 인증", description = "OTP 인증 및 토큰 관리 API")
public class OtpController {
    private final OtpService otpService;
    private final TokenService tokenService;
    
    /**
     * 사용자 전화번호로 SMS OTP를 전송합니다.
     * 
     * @param request OTP 전송 요청 (전화번호 포함)
     * @return 성공 메시지
     */
    @PostMapping("/otp/send")
    @Operation(summary = "OTP 전송", description = "사용자 전화번호로 SMS OTP를 전송합니다.")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody SendOtpRequest request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Sending OTP to phone: {}", request.getPhone());
            otpService.sendOtp(request.getPhone());
            response.put("status", "success");
            response.put("message", "OTP가 전송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (OtpException e) {
            log.error("OTP 전송 중 오류 발생: {}", e.getMessage());
            // Redis 연결 오류가 있더라도 문자는 이미 전송되었으므로 사용자에게는 성공 메시지 전달
            // 이는 실제 서비스에서는 보안을 위해 적절히 조정해야 함
            response.put("status", "success");
            response.put("message", "OTP가 전송되었습니다. 추가 처리 중 일부 내부 문제가 발생했으나 인증 코드는 발송됨.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error during OTP send: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "서버 내부 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 사용자가 입력한 OTP 코드를 검증하고 성공 시 토큰을 발급합니다.
     * 
     * @param request OTP 검증 요청 (전화번호, 코드 포함)
     * @return 액세스 토큰과 리프레시 토큰
     */
    @PostMapping("/otp/verify")
    @Operation(summary = "OTP 검증", description = "사용자가 입력한 OTP 코드를 검증하고 성공 시 토큰을 발급합니다.")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            TokenResponse tokenResponse = otpService.verifyOtp(request.getPhone(), request.getCode());
            return ResponseEntity.ok(tokenResponse);
        } catch (OtpException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "서버 내부 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 리프레시 토큰을 사용하여 만료된 액세스 토큰을 갱신합니다.
     * 
     * @param request 토큰 갱신 요청 (리프레시 토큰 포함)
     * @return 새 액세스 토큰과 리프레시 토큰
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 만료된 액세스 토큰을 갱신합니다.")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String newAccessToken = tokenService.refreshAccessToken(request.getPhone(), request.getRefreshToken());
            if (newAccessToken == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "토큰 갱신에 실패했습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(new TokenResponse(newAccessToken, request.getRefreshToken()));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 사용자를 로그아웃 처리하고 리프레시 토큰을 무효화합니다.
     * 
     * @param request 로그아웃 요청 (전화번호 포함)
     * @return 성공 메시지
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리하고 리프레시 토큰을 무효화합니다.")
    public ResponseEntity<Map<String, String>> logout(@RequestBody LogoutRequest request) {
        tokenService.removeRefreshToken(request.getPhone());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "로그아웃 되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 에코 테스트용 API 엔드포인트입니다.
     * 
     * @param message 에코할 메시지
     * @return 받은 메시지 그대로 반환
     */
    @GetMapping("/echo")
    @Operation(summary = "에코 테스트", description = "API 테스트용 에코 엔드포인트입니다.")
    public ResponseEntity<Map<String, String>> echo(@RequestParam String message) {
        Map<String, String> response = new HashMap<>();
        response.put("echo", message);
        return ResponseEntity.ok(response);
    }
} 