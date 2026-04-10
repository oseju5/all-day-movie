package com.yonsai.backend.service;

import com.yonsai.backend.dto.SeatResponseDto;
import java.util.List;

/**
 * 좌석 상태 조회 서비스 인터페이스.
 *
 * @author ohseju
 * @since 2026-03-31
 */
public interface SeatService {

    /**
     * 특정 상영 회차의 가상 좌석 및 상태(예매 여부)를 반환합니다.
     */
    List<SeatResponseDto> getSeatsWithStatus(Long showtimeId);

}
