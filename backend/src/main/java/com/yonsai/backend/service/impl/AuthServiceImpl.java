package com.yonsai.backend.service.impl;

import com.yonsai.backend.dto.LoginRequestDto;
import com.yonsai.backend.dto.SignupRequestDto;
import com.yonsai.backend.dto.TokenResponseDto;
import com.yonsai.backend.entity.User;
import com.yonsai.backend.repository.UserRepository;
import com.yonsai.backend.security.JwtTokenProvider;
import com.yonsai.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * 인증 및 계정 관련 서비스 구현체.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public void registerUser(SignupRequestDto requestDto) {
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = new User();
        user.setUsername(requestDto.getUsername());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setNickname(requestDto.getNickname());
        user.setPhone(requestDto.getPhone());
        user.setEmail(requestDto.getEmail());
        user.setRole("ROLE_USER");
        
        try {
            user.setBirthDate(LocalDate.parse(requestDto.getBirthDate()));
        } catch (Exception e) {
            throw new IllegalArgumentException("생년월일 형식이 올바르지 않습니다.");
        }

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto login(LoginRequestDto requestDto) {
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), user.getPassword(), 
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );

        // 성인 여부 계산 (만 19세 이상)
        boolean isAdult = false;
        if (user.getBirthDate() != null) {
            int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
            isAdult = age >= 19;
        }

        // JWT 발급 (닉네임 및 성인 여부 포함)
        String token = jwtTokenProvider.createToken(authentication, user.getNickname(), isAdult);

        // TokenResponseDto 생성 시 username 포함하여 반환
        return new TokenResponseDto(token, user.getNickname(), user.getUsername(), isAdult);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public String findUsernameByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다."));
        return user.getUsername();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyUserForPasswordReset(String username, String email) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return user.getEmail().equals(email);
    }

    @Override
    @Transactional
    public void resetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
