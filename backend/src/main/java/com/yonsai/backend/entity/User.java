package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 서비스 이용자(회원) 정보를 담는 엔티티.
 * * @author : ohseju
 * @since : 2026-03-17
 */

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User extends BaseEntity{
	
	/** 고유 식별 ID (자동 증가) */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/** 유저 아이디 (로그인 식별용, Unique) */
	@Column(unique = true, nullable = false, length = 50)
	private String username;
	
	/** 비밀번호 (암호화된 문자열 저장) */
	@Column(nullable = false)
    private String password;
	
	/** 서비스 내에서 표시될 활동명 */
	@Column(nullable = false, length = 50)
    private String nickname;
	
	/** 연락처 정보 */
	@Column(nullable = false, length = 20)
    private String phone;

	/** 이메일 주소 (Unique) */
    @Column(unique = true, nullable = false, length = 100)
    private String email;
	
    /** 생년월일 (성인 인증 등에 활용) */
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** 권한 (예: ROLE_USER, ROLE_ADMIN) */
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";

}
