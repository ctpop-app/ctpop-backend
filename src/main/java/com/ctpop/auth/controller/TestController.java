package com.ctpop.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 테스트용 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

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
} 