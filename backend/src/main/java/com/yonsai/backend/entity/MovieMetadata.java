package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영화의 OTT 링크와 포스터 이미지를 저장하는 엔티티.
 * 크롤링 최소화를 위한 DB 캐시 용도로 사용됩니다.
 * 
 * @author : ohseju
 * @since : 2026-04-10
 */
@Entity
@Table(name = "movie_metadata")
@Getter @Setter @NoArgsConstructor
public class MovieMetadata extends BaseEntity {

    @Id
    @Column(name = "movie_title", length = 200)
    private String movieTitle;

    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;

    @Column(name = "ott_link", columnDefinition = "TEXT")
    private String ottLink;

    @Column(name = "ott_name", length = 50)
    private String ottName;

    @Column(name = "is_valid")
    private boolean isValid = true;
}