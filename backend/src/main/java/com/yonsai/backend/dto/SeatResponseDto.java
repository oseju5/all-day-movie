package com.yonsai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 프론트엔드로 전달되는 개별 좌석 상태 DTO.
 *
 * @author ohseju
 * @since 2026-03-31
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponseDto {
    private String seatNumber;
    private String status; // "AVAILABLE", "PENDING", "RESERVED"
    private Long userId;   // "PENDING" 상태일 때 누가 선점했는지 식별용 (본인 좌석인지 확인)
}
