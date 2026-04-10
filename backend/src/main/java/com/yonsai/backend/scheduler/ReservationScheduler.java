package com.yonsai.backend.scheduler;

import com.yonsai.backend.entity.Reservation;
import com.yonsai.backend.entity.Ticket;
import com.yonsai.backend.repository.ReservationRepository;
import com.yonsai.backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;

    /**
     * 상영 시간이 지난 영화표 상태를 EXPIRED(만료됨)로 업데이트.
     * 상영 시간은 12:00, 15:00, 18:00, 21:00 이므로, 
     * 해당 시간이 지난 직후(12시 1분, 15시 1분, 18시 1분, 21시 1분)에 실행하도록 크론식을 설정.
     * "ALL_CANCELLED" 상태인 예약은 무시.
     */
    @Scheduled(cron = "0 1 12,15,18,21 * * *")
    @Transactional
    public void expireOldReservations() {
        log.info("[Scheduler] 상영 시간이 지난 예매 내역 만료(EXPIRED) 처리 시작...");

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        List<Reservation> allReservations = reservationRepository.findAll();

        int expireCount = 0;

        for (Reservation res : allReservations) {
            // 이미 전체 취소됐거나 만료된 것은 패스
            if ("ALL_CANCELLED".equals(res.getStatus()) || "EXPIRED".equals(res.getStatus())) {
                continue;
            }

            String startDay = res.getShowtime().getStartDay(); // "yyyy-MM-dd"
            String startTime = res.getShowtime().getStartTime(); // "HH:mm"

            boolean isExpired = false;

            if (startDay.compareTo(today) < 0) {
                // 상영일이 어제 이전인 경우
                isExpired = true;
            } else if (startDay.equals(today) && startTime.compareTo(currentTime) < 0) {
                // 상영일이 오늘인데 이미 상영 시간이 지난 경우
                isExpired = true;
            }

            if (isExpired) {
                // 예약 상태 만료
                res.setStatus("EXPIRED");
                reservationRepository.save(res);
                
                // 관련 티켓 중 취소되지 않은 티켓도 만료
                List<Ticket> tickets = ticketRepository.findByReservation(res);
                for (Ticket ticket : tickets) {
                    if (!"CANCELLED".equals(ticket.getStatus())) {
                        ticket.setStatus("EXPIRED");
                        ticketRepository.save(ticket);
                    }
                }
                expireCount++;
            }
        }

        log.info("[Scheduler] 상영 시간이 지난 예매 내역 {}건을 만료(EXPIRED) 처리 완료했습니다.", expireCount);
    }
}
