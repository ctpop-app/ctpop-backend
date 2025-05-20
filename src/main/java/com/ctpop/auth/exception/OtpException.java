package com.ctpop.auth.exception;

/**
 * OTP 인증 과정에서 발생하는 예외를 처리하는 클래스
 * 
 * 이 예외는 다음과 같은 상황에서 발생할 수 있습니다:
 * - 전화번호 형식이 올바르지 않을 때
 * - Twilio API 호출 중 오류가 발생할 때
 * - OTP가 만료되었을 때
 * - OTP 코드가 일치하지 않을 때
 * 
 * RuntimeException을 상속받아 checked exception 처리 없이 사용할 수 있습니다.
 */
public class OtpException extends RuntimeException {
    /**
     * 지정된 오류 메시지로 OtpException을 생성합니다.
     * 
     * @param message 예외에 대한 상세 설명
     */
    public OtpException(String message) {
        super(message);
    }
    
    /**
     * 지정된 오류 메시지와 원인 예외로 OtpException을 생성합니다.
     * 
     * @param message 예외에 대한 상세 설명
     * @param cause 이 예외의 원인이 된 예외
     */
    public OtpException(String message, Throwable cause) {
        super(message, cause);
    }
} 