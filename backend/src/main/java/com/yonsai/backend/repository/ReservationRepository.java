package com.yonsai.backend.repository;

import com.yonsai.backend.entity.Reservation;
import com.yonsai.backend.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 예매 마스터 정보 Repository.
 *
 * @author ohseju
 * @since 2026-04-01
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 특정 사용자의 모든 예매 내역을 최신순으로 조회합니다. (Fetch Join 적용)
     * N+1 문제를 해결하고 지연 로딩 예외를 방지하기 위해 연관 엔티티를 함께 조회합니다.
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.showtime s JOIN FETCH s.movie JOIN FETCH s.screen WHERE r.user = :user ORDER BY r.createdAt DESC")
    List<Reservation> findByUserWithDetails(@Param("user") User user);

}
