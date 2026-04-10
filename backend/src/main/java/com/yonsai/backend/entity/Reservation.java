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
 * 사용자의 전체 영화 예매 내역을 관리하는 마스터 엔티티.
 * 한 번의 예매 결제 단위로 생성되며, 여러 개의 티켓을 포함할 수 있습니다.
 *
 * @author ohseju
 * @since 2026-04-01
 */
@Entity
@Table(name = "Reservations")
@Getter @Setter @NoArgsConstructor
public class Reservation extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(name = "adult_count", nullable = false)
    private Integer adultCount;

    @Column(name = "youth_count", nullable = false)
    private Integer youthCount;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "cancel_price")
    private Integer cancelPrice = 0;

    @Column(nullable = false, length = 20)
    private String status; 
    
    // Reservation 삭제 시 관련 티켓(Ticket) 연쇄 삭제
    @jakarta.persistence.OneToMany(mappedBy = "reservation", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<Ticket> tickets;

    // Reservation 삭제 시 관련 결제(Payment) 연쇄 삭제
    @jakarta.persistence.OneToMany(mappedBy = "reservation", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<Payment> payments;
}
