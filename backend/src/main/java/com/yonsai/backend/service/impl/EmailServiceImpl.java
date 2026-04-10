package com.yonsai.backend.service.impl;

import com.yonsai.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 서비스 구현체. JavaMailSender를 활용.
 * ConcurrentHashMap에 인증번호를 저장합니다.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    
    // 이메일을 키로, {인증번호, 생성시간}을 값으로 저장하는 인메모리 저장소
    private final Map<String, VerificationInfo> verificationCodes = new ConcurrentHashMap<>();

    // 인증번호 만료 시간 (3분 = 180,000ms)
    private static final long EXPIRATION_TIME = 3 * 60 * 1000;

    @Override
    public String sendVerificationCode(String email) {
        String code = createRandomCode();
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[AllDayMovie] 이메일 인증번호 안내");
            message.setText("인증번호는 [" + code + "] 입니다. \n인증번호를 폼에 입력해주세요.\n(3분 이내에 입력해주세요.)");
            
            javaMailSender.send(message);
            verificationCodes.put(email, new VerificationInfo(code, System.currentTimeMillis()));
            
            log.info("Email sent to: {}, code: {}", email, code);
            return code;
        } catch (Exception e) {
            log.error("메일 발송 실패", e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        VerificationInfo info = verificationCodes.get(email);
        
        if (info == null) return false;

        // 시간 만료 여부 확인
        if (System.currentTimeMillis() - info.getCreatedAt() > EXPIRATION_TIME) {
            verificationCodes.remove(email);
            return false;
        }

        if (info.getCode().equals(code)) {
            verificationCodes.remove(email); // 인증 성공 시 삭제
            return true;
        }
        return false;
    }

    // 인증 정보 저장을 위한 내부 클래스
    @RequiredArgsConstructor
    @lombok.Getter
    private static class VerificationInfo {
        private final String code;
        private final long createdAt;
    }

    private String createRandomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
