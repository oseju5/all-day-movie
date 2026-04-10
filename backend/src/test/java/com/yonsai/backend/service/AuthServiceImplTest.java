package com.yonsai.backend.service;

import com.yonsai.backend.dto.LoginRequestDto;
import com.yonsai.backend.dto.SignupRequestDto;
import com.yonsai.backend.dto.TokenResponseDto;
import com.yonsai.backend.entity.User;
import com.yonsai.backend.repository.UserRepository;
import com.yonsai.backend.security.JwtTokenProvider;
import com.yonsai.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequestDto signupRequestDto;
    private LoginRequestDto loginRequestDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        signupRequestDto = new SignupRequestDto();
        signupRequestDto.setUsername("testuser");
        signupRequestDto.setPassword("password123");
        signupRequestDto.setNickname("tester");
        signupRequestDto.setEmail("test@example.com");
        signupRequestDto.setPhone("010-1234-5678");
        signupRequestDto.setBirthDate("1995-05-05");

        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("testuser");
        loginRequestDto.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword123");
        testUser.setNickname("tester");
        testUser.setRole("ROLE_USER");
        testUser.setBirthDate(LocalDate.of(1995, 5, 5));
    }

    @Test
    @DisplayName("회원가입 성공 - 비밀번호가 암호화되어 저장되는지 확인")
    void registerUser_success() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");

        // When
        authService.registerUser(signupRequestDto);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword123"); // 암호화된 비밀번호 저장 여부 검증
        assertThat(savedUser.getNickname()).isEqualTo("tester");
        assertThat(savedUser.getRole()).isEqualTo("ROLE_USER");
        assertThat(savedUser.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 5));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 아이디 가입 시도 시 예외 발생")
    void registerUser_fail_duplicateUsername() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerUser(signupRequestDto);
        });

        assertThat(exception.getMessage()).isEqualTo("이미 사용중인 아이디입니다.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 - 토큰 및 회원 정보(nickname 등) 반환 검증")
    void login_success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        // login 메서드 내에서 Authentication 객체를 직접 생성하므로, Mockito가 이를 감지할 수 있도록
        when(jwtTokenProvider.createToken(any(Authentication.class), anyString(), anyBoolean())).thenReturn("mocked-jwt-token");
        // When
        TokenResponseDto response = authService.login(loginRequestDto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getNickname()).isEqualTo("tester");
        // 1995년생이므로 성인(isAdult = true)인지 확인
        // isAdult()가 아닌 getIsAdult() 또는 Lombok Getter padrão에 따라 달라질 수 있음
        assertThat(response.isAdult()).isTrue();    
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 예외 발생")
    void login_fail_passwordMismatch() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(loginRequestDto);
        });

        assertThat(exception.getMessage()).isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
        verify(jwtTokenProvider, never()).createToken(any(Authentication.class), anyString(), anyBoolean());
    }
}
