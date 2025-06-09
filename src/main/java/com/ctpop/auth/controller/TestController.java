package com.ctpop.auth.controller;

import com.ctpop.auth.dto.response.TokenResponse;
import com.ctpop.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 테스트용 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final TokenService tokenService;

    /**
     * 서버 연결 테스트를 위한 에코 엔드포인트
     * @param message 테스트 메시지
     * @return 입력받은 메시지를 그대로 반환
     */
    @GetMapping("/echo")
    public ResponseEntity<String> echo(@RequestParam(required = false) String message) {
        log.info("서버 연결 테스트 요청 수신 - 메시지: {}", message);
        String response = message != null ? message : "echo";
        log.info("서버 연결 테스트 응답 전송 - 응답: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/superpass")
    public ResponseEntity<TokenResponse> getSuperPassToken(@RequestHeader("Authorization") String authorization) {
        // Bearer 토큰에서 UUID 추출
        String uuid = authorization.replace("Bearer ", "");
        String refreshToken = tokenService.generateRefreshToken(uuid);
        return ResponseEntity.ok(new TokenResponse(null, refreshToken, uuid));
    }
} 