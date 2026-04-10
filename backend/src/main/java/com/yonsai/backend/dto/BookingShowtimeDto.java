package com.yonsai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예매 페이지에서 회차 정보를 표시하기 위한 DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingShowtimeDto {
    private Long id;
    private String startTime;
    private String endTime;
    private int totalSeats;
    private int remainingSeats;
    private String screenName;
}
