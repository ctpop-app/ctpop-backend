package com.ctpop.auth.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Twilio API 연동에 필요한 설정 정보를 관리하는 클래스
 * 
 * application.yml 파일의 app.twilio 프로퍼티를 자동으로 바인딩합니다.
 * Twilio API를 사용하여 SMS OTP를 발송하는 데 필요한 계정 정보 및 서비스 ID를 포함합니다.
 * 
 * 설정 정보:
 * - accountSid: Twilio 계정 식별자
 * - authToken: Twilio API 인증 토큰
 * - verifySid: Twilio Verify 서비스 식별자
 * 
 * 애플리케이션 시작 시 Twilio 클라이언트를 자동으로 초기화합니다.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.twilio")
public class TwilioConfig {
    // Twilio 계정 식별자 (.env 파일의 TWILIO_SID를 사용)
    private String accountSid;
    
    // Twilio API 인증 토큰 (.env 파일의 TWILIO_TOKEN을 사용)
    private String authToken;
    
    // Twilio Verify 서비스 식별자 (.env 파일의 TWILIO_VERIFY_SID를 사용)
    private String verifySid;
    
    /**
     * 애플리케이션 시작 시 Twilio 클라이언트를 초기화합니다.
     * 설정된 accountSid와 authToken을 사용하여 Twilio API에 인증합니다.
     */
    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }
} 