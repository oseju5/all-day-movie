package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KMDb API로부터 수집된 영화 정보를 저장하는 엔티티.
 * * @author : ohseju
 * @since : 2026-03-17
 */

@Entity
@Table(name = "movies")
@Getter @Setter @NoArgsConstructor
public class Movie extends BaseEntity{

	/** KMDb에서 사용하는 영화 고유 식별자 */
	@Id
    @Column(length = 10)
    private String docid;
	
	/** 영화 제목 */
	@Column(nullable = false, length = 200)
    private String title;

    /** 영화 줄거리 (가변 길이 텍스트) */
    @Column(columnDefinition = "TEXT")
    private String plot;

    /** 영화 상영 시간 (단위: 분) */
    private Integer runtime;

    /** 관람 등급 (예: 15세 이상 관람가) */
    @Column(length = 30)
    private String rating;

    /** 영화 개봉일 (YYYYMMDD 형식) */
    @Column(name = "release_date", length = 15)
    private String releaseDate;

    /** 영화 포스터 이미지 URL 주소 */
    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;
	
    // Movie가 삭제될 때 관련 데이터들(Showtime, Actor매핑 등)도 함께 삭제되도록 연관관계(Cascade) 추가
    @jakarta.persistence.OneToMany(mappedBy = "movie", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<Showtime> showtimes;

    @jakarta.persistence.OneToMany(mappedBy = "movie", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<MovieActor> movieActors;

    @jakarta.persistence.OneToMany(mappedBy = "movie", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<MovieDirector> movieDirectors;

    @jakarta.persistence.OneToMany(mappedBy = "movie", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<MovieKeyword> movieKeywords;

}
