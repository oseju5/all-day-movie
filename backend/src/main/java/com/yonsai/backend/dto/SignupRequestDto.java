package com.yonsai.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 시 사용하는 DTO.
 * 프론트엔드 검증과 더불어 백엔드에서도 JSR-303 검증을 수행합니다.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Getter
@Setter
public class SignupRequestDto {

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "아이디는 4~20자리의 영문자와 숫자만 사용 가능합니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^(?:(?=.*[a-zA-Z])(?=.*\\d)|(?=.*[a-zA-Z])(?=.*[@$!%*?&])|(?=.*\\d)(?=.*[@$!%*?&]))[A-Za-z\\d@$!%*?&]{8,16}$",
             message = "비밀번호는 8~16자의 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    @Size(max = 20, message = "닉네임은 최대 20자까지 입력 가능합니다.")
    private String nickname;

    private String phone;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "생년월일은 필수 입력 값입니다.")
    private String birthDate;
}
