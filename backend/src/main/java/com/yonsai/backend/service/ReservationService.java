package com.yonsai.backend.service;

import com.yonsai.backend.dto.ReservationResponseDto;
import com.yonsai.backend.entity.Reservation;
import java.util.List;

/**
 * 예매 및 티켓 마스터 처리를 위한 서비스 인터페이스.
 *
 * @author ohseju
 * @since 2026-04-01
 */
public interface ReservationService {

    /**
     * 사용자가 선택한 좌석들에 대해 예매 및 티켓을 생성합니다.
     * 동시성 발생 시 DataIntegrityViolationException 예외를 통해 롤백됩니다.
     */
    Reservation bookSeats(String username, Long showtimeId, List<String> seatNumbers, int adultCount, int youthCount);

    /**
     * 특정 사용자의 모든 예매 내역을 최신순으로 조회합니다. (DTO 반환)
     */
    List<ReservationResponseDto> getMyReservations(String username);

    /**
     * 예매 내역 중 선택된 티켓들을 취소 처리합니다.
     */
    void cancelTickets(Long reservationId, List<Long> ticketIds, int adultCancelCount, int youthCancelCount, String username);

}
