package com.yonsai.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 좌석 락(선점) 또는 해제를 요청하는 DTO.
 *
 * @author ohseju
 * @since 2026-03-31
 */
@Getter
@Setter
public class SeatLockRequestDto {
    private Long showtimeId;
    private String seatNumber;
    private Long userId;
}
