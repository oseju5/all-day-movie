package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 예매 내역 내의 개별 좌석별 티켓 정보를 관리하는 엔티티.
 * 한 건의 예매(Reservation)에 속한 구체적인 좌석 및 티켓 상태를 나타냅니다.
 *
 * @author ohseju
 * @since 2026-03-31
 */
@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor
public class Ticket extends BaseEntity{

    /** 고유 식별 ID (자동 증가 PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속된 전체 예매 내역 정보 (외래키: reservations_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservations_id", nullable = false)
    private Reservation reservation;

    /** 해당 좌석의 상영 회차 정보 (부분 유니크 인덱스 생성 및 조회 최적화용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    /** 예매된 개별 좌석 번호 (예: A1, B10 등) */
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    /** 해당 좌석 예매 시 적용된 금액 */
    @Column(nullable = false)
    private Integer price;
    
    /** 티켓 상태 (ACTIVE: 정상, CANCELLED: 취소/환불) */
    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, CANCELLED
    
}
