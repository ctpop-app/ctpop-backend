package com.ctpop.auth.service;

import com.ctpop.auth.config.TwilioConfig;
import com.ctpop.auth.dto.response.TokenResponse;
import com.ctpop.auth.exception.OtpException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import com.ctpop.auth.config.JwtConfig;
import com.ctpop.auth.exception.TokenException;

import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.UUID;

/**
 * OTP 인증 관련 비즈니스 로직을 처리하는 서비스
 *
 * 이 서비스는 다음과 같은 기능을 제공합니다:
 * 1. Twilio Verify 서비스를 통한 SMS OTP 전송
 * 2. 사용자가 입력한 OTP 코드 검증
 * 3. 검증 성공 시 JWT 토큰 발급 (TokenService 활용)
 * 4. Redis를 사용한 OTP 상태 관리 (만료 시간 적용)
 *
 * 주요 워크플로우:
 * 1. 사용자가 전화번호 입력 → OTP 코드 발송 (sendOtp)
 * 2. 사용자가 OTP 코드 입력 → 코드 검증 및 토큰 발급 (verifyOtp)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    private final RedisTemplate<String, String> redisTemplate;
    private final TwilioConfig twilioConfig;
    private final TokenService tokenService;
    private final JwtConfig jwtConfig;

    // Redis에 저장할 OTP 관련 키의 접두사
    private static final String OTP_PREFIX = "otp:";
    // OTP 코드의 유효 시간 (분 단위)
    private static final int OTP_EXPIRATION_MINUTES = 5;

    /**
     * 전화번호를 E.164 국제 형식으로 변환합니다.
     * E.164 형식은 Twilio API에서 요구하는 전화번호 형식입니다.
     * 
     * 예시: 
     * - 01027362040 → +821027362040
     * - 010-2736-2040 → +821027362040
     * - +821027362040 → +821027362040 (그대로 유지)
     * 
     * @param phone 변환할 전화번호
     * @return E.164 형식으로 변환된 전화번호
     * @throws OtpException 전화번호 형식이 올바르지 않은 경우
     */
    private String toInternationalFormat(String phone) {
        // 하이픈 제거
        phone = phone.replace("-", "");
        
        // 기본적인 형식 검증 (숫자만 있는지)
        if (!phone.matches("\\d+")) {
            throw new OtpException("전화번호는 숫자만 포함해야 합니다.");
        }
        
        // 010으로 시작하는 경우 +82로 변환
        if (phone.startsWith("010")) {
            return "+82" + phone.substring(1);
        }
        
        // 이미 +82로 시작하는 경우 그대로 반환
        if (phone.startsWith("+82")) {
            return phone;
        }
        
        // 그 외의 경우 +82를 추가
        return "+82" + phone;
    }

    /**
     * 사용자의 전화번호로 OTP를 전송합니다.
     * 
     * 처리 과정:
     * 1. 전화번호를 E.164 국제 형식으로 변환
     * 2. Twilio Verify 서비스를 통해 SMS로 OTP 전송
     * 3. Redis에 전화번호와 상태를 저장 (5분 유효)
     * 4. 액세스 토큰 생성 및 반환
     * 
     * 저장 형식: 
     * - 키: "otp:{전화번호}"
     * - 값: "pending"
     * - 만료 시간: 5분
     * 
     * @param phone 사용자 전화번호 (예: 010-2736-2040)
     * @return TokenResponse 액세스 토큰이 포함된 응답
     * @throws OtpException 전화번호 형식이 올바르지 않거나 OTP 전송에 실패한 경우
     */
    public TokenResponse sendOtp(String phone) {
        try {
            // 전화번호 형식 검증 및 변환
            String internationalPhone = toInternationalFormat(phone);
            
            // Twilio Verify 서비스를 통해 OTP 전송
            Verification verification = Verification.creator(
                twilioConfig.getVerifySid(),
                internationalPhone,
                "sms"
            ).create();
            
            log.info("Verification: {}", verification);
            log.info("Twilio status: {}", verification.getStatus());
            log.info("Twilio SID: {}", verification.getSid());
            log.info("To: {}", verification.getTo());

            // Redis에 전화번호 저장 (5분 유효)
            try {
                String key = OTP_PREFIX + phone;
                redisTemplate.opsForValue().set(key, "pending", OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
            } catch (RedisConnectionFailureException e) {
                // Redis 연결 실패 시 로그만 남기고 진행(문자는 이미 발송됨)
                log.error("Redis 연결 실패: {}. OTP는 발송되었으나 상태 저장에 실패했습니다.", e.getMessage());
            }
            
            // UUID 생성 및 액세스 토큰 발급
            String uuid = UUID.randomUUID().toString();
            String accessToken = tokenService.generateAccessToken(uuid);
            
            return new TokenResponse(accessToken, null, uuid);
            
        } catch (Exception e) {
            log.error("OTP 전송 중 오류 발생: {}", e.getMessage());
            throw new OtpException("OTP 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * OTP를 검증하고 토큰을 발급합니다.
     */
    public TokenResponse verifyOtp(String phone, String code, String accessToken) {
        try {
            log.info("OTP 검증 시작 - 전화번호: {}, 코드: {}", phone, code);
            
            // 액세스 토큰 검증
            String uuid = tokenService.validateAccessToken(accessToken);
            log.info("액세스 토큰 검증 성공 - UUID: {}", uuid);
            
            // 전화번호 형식 변환
            String internationalPhone = toInternationalFormat(phone);
            log.info("전화번호 형식 변환: {} -> {}", phone, internationalPhone);
            
            // Twilio를 통한 OTP 검증
            log.info("Twilio verification check 요청 시작");
            VerificationCheck verificationCheck = VerificationCheck.creator(
                twilioConfig.getVerifySid()
            )
            .setTo(internationalPhone)
            .setCode(code)
            .create();

            log.info("Verification Check 응답: {}", verificationCheck);
            log.info("Verification Status: {}", verificationCheck.getStatus());
            log.info("Verification Valid: {}", verificationCheck.getValid());
            
            if (!"approved".equals(verificationCheck.getStatus())) {
                log.warn("OTP 검증 실패 - 상태: {}", verificationCheck.getStatus());
                throw new OtpException("잘못된 인증번호입니다.");
            }
            
            log.info("OTP 검증 성공");
                
            // OTP 검증 성공 시 리프레시 토큰만 발급
            String refreshToken = tokenService.generateRefreshToken(uuid);
            log.info("리프레시 토큰 발급 완료");
                
            // Redis에서 OTP 상태 삭제
            String otpKey = OTP_PREFIX + phone;
            redisTemplate.delete(otpKey);
            log.info("Redis OTP 상태 삭제 완료");
                
            return new TokenResponse(null, refreshToken, uuid);
        } catch (TokenException e) {
            log.error("액세스 토큰 검증 실패: {}", e.getMessage());
            throw new OtpException("유효하지 않은 액세스 토큰입니다.");
        } catch (Exception e) {
            log.error("OTP 검증 중 오류 발생: {}", e.getMessage(), e);
            throw new OtpException("OTP 검증에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * Redis 서버가 연결 불가능한 상태인지 확인합니다.
     * 
     * @return Redis 서버가 연결 불가능한 경우 true, 그렇지 않으면 false
     */
    private boolean isRedisUnavailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return false;
        } catch (Exception e) {
            log.warn("Redis 서버가 연결 불가능한 상태입니다: {}", e.getMessage());
            return true;
        }
    }
} 