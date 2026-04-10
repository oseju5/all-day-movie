package com.yonsai.backend.service;

/**
 * 이메일 인증 관련 서비스 인터페이스.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
public interface EmailService {

    /**
     * 회원가입, 아이디/비밀번호 찾기 시 이메일로 인증번호 6자리를 전송합니다.
     * @param email 수신자 이메일 주소
     * @return 발송된 인증번호
     */
    String sendVerificationCode(String email);

    /**
     * 인증번호가 일치하는지 확인합니다.
     * @param email 수신자 이메일 주소
     * @param code 입력된 인증번호
     * @return 일치 여부
     */
    boolean verifyCode(String email, String code);
}
