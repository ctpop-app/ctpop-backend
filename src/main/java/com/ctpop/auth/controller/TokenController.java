package com.ctpop.auth.controller;

import com.ctpop.auth.dto.request.TokenRequest;
import com.ctpop.auth.dto.response.TokenResponse;
import com.ctpop.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 토큰 관리 API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TokenController {
    private final TokenService tokenService;

    /**
     * 리프레시 토큰 검증 및 재발급
     *
     * accessToken은 반환하지 않고, refreshToken과 uuid만 반환
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> validateToken(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(tokenService.validateToken(request));
    }

    /**
     * 로그아웃합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody TokenRequest request) {
        tokenService.logout(request);
        return ResponseEntity.ok().build();
    }
} 