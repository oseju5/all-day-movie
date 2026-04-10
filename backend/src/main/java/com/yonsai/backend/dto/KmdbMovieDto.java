package com.yonsai.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * KMDb API에서 받아온 영화 데이터를 매핑하기 위한 DTO.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Getter
@Setter
public class KmdbMovieDto {
    private String docid;
    private String title;
    private String plotText;
    private String runtime;
    private String rating;
    private String releaseDate;
    private String posters;
    
    // 감독 목록
    private String directors;
    
    // 배우 목록
    private String actors;
    
    // 키워드
    private String keywords;
}
