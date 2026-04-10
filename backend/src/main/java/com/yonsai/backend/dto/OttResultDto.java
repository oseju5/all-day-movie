package com.yonsai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 네이버 영화 및 OTT 크롤링 결과를 담는 DTO.
 * @author ohseju
 * @since : 2026-04-10
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OttResultDto {
    private String ottName;
    private String ottLink;
    private String posterUrl;
}
