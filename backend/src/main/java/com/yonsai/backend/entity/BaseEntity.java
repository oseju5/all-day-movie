package com.yonsai.backend.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 모든 엔티티에서 공통으로 사용하는 생성/수정 일시를 관리하는 추상 클래스.
 * * @author : ohseju
 * @since : 2026-03-17
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {
	
	/** 생성 일시 (최초 등록 시 자동 기록) */
	@CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
	
	/** 수정 일시 (변경 시 자동 업데이트) */
	@LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
