package com.yonsai.backend.service.impl;

import com.yonsai.backend.dto.SeatResponseDto;
import com.yonsai.backend.entity.Showtime;
import com.yonsai.backend.entity.Ticket;
import com.yonsai.backend.repository.ShowtimeRepository;
import com.yonsai.backend.repository.TicketRepository;
import com.yonsai.backend.service.SeatService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 상태 조회 서비스.
 * 물리적 Seat 테이블 대신 가상 좌석을 생성하고,
 * 활성화된(ACTIVE) 티켓 정보를 기반으로 RESERVED/AVAILABLE 상태를 매핑합니다.
 *
 * @author ohseju
 * @since 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {

    private final ShowtimeRepository showtimeRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponseDto> getSeatsWithStatus(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("회차 정보를 찾을 수 없습니다."));

        int totalSeats = showtime.getScreen().getTotalSeat();
        List<String> virtualSeatNumbers = generateVirtualSeats(totalSeats);

        // 확정/진행 중 예매된 티켓 좌석 조회 (ACTIVE)
        List<Ticket> activeTickets = ticketRepository.findActiveTicketsByShowtimeId(showtimeId);
        Set<String> reservedSeatNumbers = activeTickets.stream()
                .map(Ticket::getSeatNumber)
                .collect(Collectors.toSet());

        List<SeatResponseDto> responseList = new ArrayList<>();
        for (String seatNum : virtualSeatNumbers) {
            String status = reservedSeatNumbers.contains(seatNum) ? "RESERVED" : "AVAILABLE";
            
            responseList.add(SeatResponseDto.builder()
                    .seatNumber(seatNum)
                    .status(status)
                    .userId(null) // Pending 로직이 제거되었으므로 항상 null
                    .build());
        }

        return responseList;
    }

    private List<String> generateVirtualSeats(int totalSeats) {
        List<String> seatNumbers = new ArrayList<>();
        int rows = (int) Math.ceil((double) totalSeats / 10);
        
        for (int i = 0; i < rows; i++) {
            char rowChar = (char) ('A' + i);
            for (int j = 1; j <= 10; j++) {
                if (seatNumbers.size() >= totalSeats) break;
                seatNumbers.add(rowChar + String.valueOf(j));
            }
        }
        return seatNumbers;
    }
}
