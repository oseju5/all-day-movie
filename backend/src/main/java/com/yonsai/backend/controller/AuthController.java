package com.yonsai.backend.controller;

import com.yonsai.backend.dto.EmailRequestDto;
import com.yonsai.backend.dto.LoginRequestDto;
import com.yonsai.backend.dto.SignupRequestDto;
import com.yonsai.backend.dto.TokenResponseDto;
import com.yonsai.backend.dto.VerifyEmailDto;
import com.yonsai.backend.service.AuthService;
import com.yonsai.backend.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원가입, 로그인 및 인증 관련 API 컨트롤러.
 *
 * @author ohseju
 * @since : 2026-03-23
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    /**
     * 아이디 중복 검사
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam("username") String username) {
        // 아이디 양식 검증
        if (username == null || !username.matches("^[A-Za-z0-9]{4,20}$")) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            response.put("message", "아이디는 4~20자리의 영문자와 숫자만 사용 가능합니다.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean exists = authService.checkUsername(username);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        try {
            authService.registerUser(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto) {
        try {
            TokenResponseDto tokenResponse = authService.login(requestDto);
            return ResponseEntity.ok(tokenResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * 이메일 인증번호 전송
     */
    @PostMapping("/email/send-code")
    public ResponseEntity<String> sendEmailCode(@Valid @RequestBody EmailRequestDto requestDto) {
        try {
            emailService.sendVerificationCode(requestDto.getEmail());
            return ResponseEntity.ok("인증번호가 이메일로 전송되었습니다.");
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이메일 전송에 실패했습니다.");
        }
    }

    /**
     * 이메일 인증번호 확인 (아이디 찾기 시 활용 가능)
     */
    @PostMapping("/email/verify-code")
    public ResponseEntity<Map<String, Boolean>> verifyEmailCode(@Valid @RequestBody VerifyEmailDto requestDto) {
        boolean verified = emailService.verifyCode(requestDto.getEmail(), requestDto.getCode());
        Map<String, Boolean> response = new HashMap<>();
        response.put("verified", verified);
        return ResponseEntity.ok(response);
    }

    /**
     * 이메일로 아이디 찾기 (클라이언트가 인증 성공 후 이 API 호출)
     */
    @PostMapping("/find-id")
    public ResponseEntity<String> findId(@Valid @RequestBody EmailRequestDto requestDto) {
        try {
            String username = authService.findUsernameByEmail(requestDto.getEmail());
            return ResponseEntity.ok(username);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 비밀번호 재설정을 위한 계정 확인 (아이디 + 이메일)
     */
    @PostMapping("/verify-user-for-reset")
    public ResponseEntity<Map<String, Boolean>> verifyUserForReset(
            @RequestParam("username") String username, 
            @RequestParam("email") String email) {
        try {
            boolean isValid = authService.verifyUserForPasswordReset(username, email);
            Map<String, Boolean> response = new HashMap<>();
            response.put("valid", isValid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Boolean> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam("username") String username, 
            @RequestParam("newPassword") String newPassword) {
        try {
            authService.resetPassword(username, newPassword);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
