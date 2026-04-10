package com.yonsai.backend.service;

import com.yonsai.backend.dto.LoginRequestDto;
import com.yonsai.backend.dto.SignupRequestDto;
import com.yonsai.backend.dto.TokenResponseDto;

/**
 * 인증 및 계정 관련 서비스 인터페이스.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
public interface AuthService {

    /**
     * 사용자 회원가입 처리
     */
    void registerUser(SignupRequestDto requestDto);

    /**
     * 사용자 로그인 처리 및 JWT 발급
     */
    TokenResponseDto login(LoginRequestDto requestDto);

    /**
     * 아이디 중복 확인
     */
    boolean checkUsername(String username);

    /**
     * 이메일로 아이디 찾기 (인증번호 검증 성공 시 호출)
     * @param email 가입된 이메일
     * @return 찾은 아이디 (마스킹 처리 등은 프론트 혹은 여기서 선택적으로 적용)
     */
    String findUsernameByEmail(String email);

    /**
     * 아이디와 이메일이 일치하는지 확인 (비밀번호 재설정용)
     */
    boolean verifyUserForPasswordReset(String username, String email);

    /**
     * 비밀번호 재설정
     */
    void resetPassword(String username, String newPassword);
}
