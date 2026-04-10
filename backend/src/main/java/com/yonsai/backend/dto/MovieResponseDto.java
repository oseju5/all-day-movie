package com.yonsai.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 메인 화면 등에서 영화 목록을 출력할 때 사용하는 DTO.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Getter
@Setter
public class MovieResponseDto {
    private String id;
    private String title;
    private String poster;
    private String rating;
    private Integer runtime;
}
