package com.ssafy.withy.global.service;

import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // 인증번호 만료 시간 (5분)
    private static final long CODE_EXPIRATION = 300;

    /**
     * 이메일 발송
     */
    @Async
    public void sendVerificationCode(String toEmail) {
        // 1. 인증코드 생성
        String authCode = createCode();

        // 2. MimeMessage 생성
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            // MimeMessageHelper 사용 (multipart=true, encoding=UTF-8)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[WITHY] 회원가입 인증번호 안내");
            helper.setFrom(senderEmail);

            // HTML 내용 생성
            String htmlContent = getVerificationHtml(authCode);

            // [핵심] 두 번째 인자 true가 HTML 렌더링을 켠다.
            helper.setText(htmlContent, true);

            // 3. 발송
            javaMailSender.send(message);

            // 4. Redis 저장 (Key: "AuthCode:이메일", Value: "123456", TTL: 5분)
            redisTemplate.opsForValue().set("AuthCode:" + toEmail, authCode, CODE_EXPIRATION, TimeUnit.SECONDS);
            log.info("📧 인증메일 발송 성공: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ 이메일 발송 실패 (MessagingException): {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.EMAIL_SEND_FAILED);
        } catch (Exception e) {
            log.error("❌ 이메일 발송 실패 (Unknown): {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * 인증번호 검증 로직
     */
    public boolean verifyCode(String email, String code) {
        String key = "AuthCode:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            // 인증 성공 -> Redis에서 인증코드 삭제
            redisTemplate.delete(key);
            // "Verified:이메일" 키 저장 (30분 유지) -> 회원가입 시 체크용
            redisTemplate.opsForValue().set("Verified:" + email, "true", 1800, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    // 6자리 난수 생성기
    private String createCode() {
        return String.valueOf((int) (Math.random() * 899999) + 100000);
    }

    // HTML 템플릿 생성 메서드
    private String getVerificationHtml(String authCode) {
        return "<div style='font-family: \"Apple SD Gothic Neo\", \"Malgun Gothic\", sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;'>"
                + "  <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>"
                + "    "
                + "    <div style='background-color: #333333; padding: 30px 0; text-align: center;'>"
                + "      <h1 style='color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px;'>WITHY</h1>"
                + "    </div>"
                + "    "
                + "    <div style='padding: 40px 30px; text-align: center; color: #333333;'>"
                + "      <h2 style='font-size: 20px; margin-bottom: 20px;'>회원가입 인증번호</h2>"
                + "      <p style='font-size: 16px; line-height: 1.6; color: #666666; margin-bottom: 30px;'>"
                + "        안녕하세요, WITHY에 오신 것을 환영합니다.<br>"
                + "        아래 인증번호를 5분 이내에 입력하여 가입을 완료해주세요."
                + "      </p>"
                + "      "
                + "      <div style='background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 4px; padding: 15px; display: inline-block; margin-bottom: 30px;'>"
                + "        <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #007bff;'>" + authCode + "</span>"
                + "      </div>"
                + "      <p style='font-size: 14px; color: #999999; margin-top: 20px;'>"
                + "        본 메일은 발신 전용이며, 회신되지 않습니다."
                + "      </p>"
                + "    </div>"
                + "    "
                + "    <div style='background-color: #eeeeee; padding: 20px; text-align: center; font-size: 12px; color: #888888;'>"
                + "      &copy; 2026 WITHY Team. All rights reserved."
                + "    </div>"
                + "  </div>"
                + "</div>";
    }
}