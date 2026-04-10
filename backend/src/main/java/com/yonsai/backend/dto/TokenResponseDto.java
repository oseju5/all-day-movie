package com.yonsai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 성공 시 클라이언트로 반환하는 DTO.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Getter
@Setter
@AllArgsConstructor
public class TokenResponseDto {
    private String token;
    private String nickname;
    private String username;
    private boolean isAdult;
}
