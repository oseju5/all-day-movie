package com.yonsai.backend.repository;

import com.yonsai.backend.entity.Reservation;
import com.yonsai.backend.entity.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 티켓 상세 정보 Repository.
 *
 * @author ohseju
 * @since 2026-04-01
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * 특정 예매 마스터 정보에 속한 모든 티켓을 조회합니다.
     */
    List<Ticket> findByReservation(Reservation reservation);

    /**
     * 특정 상영 회차의 활성화된 티켓 수를 조회합니다.
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.status = 'ACTIVE'")
    int countActiveTicketsByShowtimeId(@Param("showtimeId") Long showtimeId);

    /**
     * 특정 상영 회차의 활성화된 티켓 목록을 조회합니다.
     */
    @Query("SELECT t FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.status = 'ACTIVE'")
    List<Ticket> findActiveTicketsByShowtimeId(@Param("showtimeId") Long showtimeId);

}
