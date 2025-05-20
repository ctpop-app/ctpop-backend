package com.ctpop.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * 애플리케이션 전체에서 발생하는 예외를 처리하는 글로벌 예외 핸들러
 * 
 * 이 클래스는 다음과 같은 예외 처리를 담당합니다:
 * 1. OtpException - OTP 인증 관련 예외
 * 2. 그 외 예상치 못한 예외
 * 
 * 예외가 발생하면 적절한 HTTP 상태 코드와 함께 일관된 형식의 오류 응답을 반환합니다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * OtpException 예외 처리
     * 
     * OTP 인증 과정에서 발생한 예외를 400 Bad Request 상태 코드와 함께 반환합니다.
     * 
     * @param ex 발생한 OtpException 객체
     * @return 오류 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<?> handleOtpException(OtpException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * 일반적인 예외 처리
     * 
     * 애플리케이션에서 발생한 처리되지 않은 예외를 500 Internal Server Error 상태 코드와 함께 반환합니다.
     * 
     * @param ex 발생한 Exception 객체
     * @return 오류 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "서버 내부 오류가 발생했습니다: " + ex.getMessage()));
    }
} 