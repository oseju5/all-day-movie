package com.yonsai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.yonsai.backend.security.JwtAuthenticationFilter;
import com.yonsai.backend.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 환경 설정 클래스.
 * JWT 기반 인증을 사용하므로 CSRF, 세션 등을 비활성화합니다.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 요구사항: BCryptPasswordEncoder로 암호화
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .httpBasic(httpBasic -> httpBasic.disable())
            .csrf(csrf -> csrf.disable()) // JWT를 사용하므로 CSRF 비활성화
            .cors(cors -> cors.configure(http)) // CORS 설정 활성화
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안 함
            .authorizeHttpRequests(auth -> auth
            	// 1. 누구나 접근 가능한 곳 (영화 조회, 로그인 등)
            	.requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/*.svg", "/images/**", "/api/auth/**", "/api/chatbot/**", "/api/movies/**","/api/seats/**", "/booking").permitAll()
            	.requestMatchers("/login", "/signup", "/mypage", "/find-account", "/booking", "/seat-selection").permitAll()
            	
            	// 2. 관리자만 접근 가능한 곳 (KMDB 수동 갱신 등)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
             // 3. 그 외 모든 요청(예약, 취소, 챗봇 상세 등)은 로그인이 필요함
                .anyRequest().authenticated()
             )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
