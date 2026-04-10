package com.yonsai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 요청 시 사용하는 DTO.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Getter
@Setter
public class LoginRequestDto {
    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
