package com.ctpop.auth.exception;

/**
 * 토큰 관련 예외
 * - 토큰 검증 실패
 * - 토큰 만료
 * - 토큰 변조
 * - 토큰 형식 오류
 */
public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
} 