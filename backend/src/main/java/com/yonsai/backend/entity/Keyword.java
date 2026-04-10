package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영화의 키워드 정보를 담는 엔티티.
 * * @author : ohseju
 * @since : 2026-03-17
 */

@Entity
@Table(name = "keywords")
@Getter @Setter @NoArgsConstructor
public class Keyword extends BaseEntity{
	
	/** 고유 식별 ID (자동 증가) */
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	/** 키워드 이름 */
    @Column(unique = true, nullable = false, length = 100)
    private String keywords;

}
