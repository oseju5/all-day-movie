package com.yonsai.backend.service.impl;

import com.yonsai.backend.dto.ReservationResponseDto;
import com.yonsai.backend.entity.Reservation;
import com.yonsai.backend.entity.Showtime;
import com.yonsai.backend.entity.Ticket;
import com.yonsai.backend.entity.User;
import com.yonsai.backend.repository.ReservationRepository;
import com.yonsai.backend.repository.ShowtimeRepository;
import com.yonsai.backend.repository.TicketRepository;
import com.yonsai.backend.repository.UserRepository;
import com.yonsai.backend.service.ReservationService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예매 및 티켓 관리를 처리하는 서비스.
 * DB 레벨의 Partial Unique Index를 활용해 동시성(중복 예매)을 제어합니다.
 *
 * @author ohseju
 * @since 2026-04-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createPartialUniqueIndex() {
        try {
            log.info("🔍 [Startup] Partial Unique Index 생성 또는 확인 시도 중...");
            String createIndexSql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_ticket " +
                                    "ON tickets(showtime_id, seat_number) " +
                                    "WHERE status = 'ACTIVE'";
            entityManager.createNativeQuery(createIndexSql).executeUpdate();
            log.info("✅ [Startup] Partial Unique Index 'idx_unique_active_ticket' 처리가 완료되었습니다.");
        } catch (Exception e) {
            log.error("❌ Partial Unique Index 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public Reservation bookSeats(String username, Long showtimeId, List<String> seatNumbers, int adultCount, int youthCount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("상영 회차를 찾을 수 없습니다."));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setShowtime(showtime);
        int totalPrice = (adultCount * 15000) + (youthCount * 12000); 
        reservation.setTotalPrice(totalPrice);
        reservation.setAdultCount(adultCount);
        reservation.setYouthCount(youthCount);
        reservation.setStatus("COMPLETED"); 
        reservation = reservationRepository.save(reservation);

        for (String seatNumber : seatNumbers) {
            Ticket ticket = new Ticket();
            ticket.setReservation(reservation);
            ticket.setShowtime(showtime);
            ticket.setSeatNumber(seatNumber);
            ticket.setStatus("ACTIVE");
            ticket.setPrice(15000); 
            
            try {
                ticketRepository.save(ticket);
            } catch (DataIntegrityViolationException e) {
                log.warn("❌ 중복 예매 감지! 좌석 번호: {}", seatNumber);
                throw new IllegalStateException("선택하신 좌석(" + seatNumber + ")은 이미 예매된 좌석입니다.");
            }
        }
        
        return reservation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getMyReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        List<Reservation> reservations = reservationRepository.findByUserWithDetails(user);
        
        return reservations.stream().map(res -> {
            List<Ticket> tickets = ticketRepository.findByReservation(res);
            
            // 1. 내부 티켓 리스트 변환을 먼저 변수로 추출 (타입 추론 도움)
            List<ReservationResponseDto.TicketResponseDto> ticketDtos = tickets.stream()
                    .map(t -> ReservationResponseDto.TicketResponseDto.builder()
                            .id(t.getId())
                            .seatNumber(t.getSeatNumber())
                            .status(t.getStatus())
                            .price(t.getPrice())
                            .build())
                    .collect(Collectors.toList());

            // 2. 전체 Dto 빌드
            return ReservationResponseDto.builder()
                    .id(res.getId())
                    .movieTitle(res.getShowtime().getMovie().getTitle())
                    .movieRating(res.getShowtime().getMovie().getRating())
                    .startDay(res.getShowtime().getStartDay())
                    .startTime(res.getShowtime().getStartTime())
                    .screenName(res.getShowtime().getScreen().getName())
                    .adultCount(res.getAdultCount())
                    .youthCount(res.getYouthCount())
                    .totalPrice(res.getTotalPrice())
                    .cancelPrice(res.getCancelPrice())
                    .status(res.getStatus())
                    .tickets(ticketDtos) // 추출한 변수 사용
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelTickets(Long reservationId, List<Long> ticketIds, int adultCancelCount, int youthCancelCount, String username) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예매 내역을 찾을 수 없습니다."));

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new SecurityException("본인의 예매 내역만 취소할 수 있습니다.");
        }

        Showtime showtime = reservation.getShowtime();
        LocalDateTime startTime = LocalDateTime.of(LocalDate.parse(showtime.getStartDay()), LocalTime.parse(showtime.getStartTime()));
        
        if (LocalDateTime.now(clock).isAfter(startTime.minusMinutes(20))) {
            throw new IllegalStateException("상영 시작 20분 전까지만 취소 가능합니다.");
        }

        List<Ticket> allTickets = ticketRepository.findByReservation(reservation);
        List<Ticket> ticketsToCancel = allTickets.stream()
                .filter(t -> ticketIds.contains(t.getId()))
                .collect(Collectors.toList());

        if (ticketsToCancel.size() != (adultCancelCount + youthCancelCount)) {
            throw new IllegalArgumentException("취소할 티켓 수와 선택된 좌석 수가 일치하지 않습니다.");
        }

        // Cancel Price 계산
        int adultPrice = reservation.getShowtime().getScreen().getPriceAdult();
        int youthPrice = reservation.getShowtime().getScreen().getPriceChild();
        int cancelAmount = (adultCancelCount * adultPrice) + (youthCancelCount * youthPrice);

        for (Ticket ticket : ticketsToCancel) {
            if ("CANCELLED".equals(ticket.getStatus())) {
                throw new IllegalStateException("이미 취소된 티켓이 포함되어 있습니다: " + ticket.getSeatNumber());
            }
            ticket.setStatus("CANCELLED");
        }
        
        Integer existingCancelPrice = reservation.getCancelPrice();
        reservation.setCancelPrice((existingCancelPrice == null ? 0 : existingCancelPrice) + cancelAmount);
        
        // adultCount, youthCount 업데이트
        reservation.setAdultCount(reservation.getAdultCount() - adultCancelCount);
        reservation.setYouthCount(reservation.getYouthCount() - youthCancelCount);
        
        long activeCount = allTickets.stream().filter(t -> "ACTIVE".equals(t.getStatus())).count();
        if (activeCount == 0) {
            reservation.setStatus("ALL_CANCELLED");
        } else {
            reservation.setStatus("PARTIAL_CANCELLED");
        }

        reservationRepository.save(reservation);
    }
}
