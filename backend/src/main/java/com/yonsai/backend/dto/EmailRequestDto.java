package com.yonsai.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 이메일 인증 요청 시 사용하는 DTO.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Getter
@Setter
public class EmailRequestDto {
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
