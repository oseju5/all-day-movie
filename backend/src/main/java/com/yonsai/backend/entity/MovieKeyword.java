package com.yonsai.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영화(Movie)와 키워드(Keyword) 간의 N:M(다대다) 관계를 해결하기 위한 매핑 엔티티.
 * 한 영화가 여러 키워드를 가질 수 있고, 한 키워드가 여러 영화에 속할 수 있는 구조를 
 * 데이터베이스의 연결 테이블(Join Table) 형태로 관리합니다.
 * * @author : ohseju
 * @since : 2026-03-17
 */
@Entity
@Getter @Setter @NoArgsConstructor
public class MovieKeyword extends BaseEntity{
	
	/** * 고유 식별 ID (시스템 관리용 자동 증가 PK)*/
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	/** * 연관된 영화 정보 (외래키: movie_id)
     * * @ManyToOne: 여러 개의 매핑 정보가 하나의 영화를 가리킵니다.
     * FetchType.LAZY: 영화 정보가 실제로 필요한 시점에만 DB에서 조회하여 성능을 최적화합니다.
     */
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
	
	/** * 연관된 키워드 정보 (외래키: keyword_id)
     * * @ManyToOne: 여러 개의 매핑 정보가 하나의 키워드를 가리킵니다.
     * FetchType.LAZY: 키워드 정보가 실제로 필요한 시점에만 DB에서 조회하여 성능을 최적화합니다.
     */
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;
	
}
