package com.yonsai.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 예매 내역에 대한 실제 결제 승인 정보를 관리하는 엔티티.
 * 외부 결제 API(카카오페이 등)와의 대조를 위한 정보를 포함합니다.
 *
 * @author : ohseju
 * @since : 2026-03-17
 */

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment extends BaseEntity{

	/** 고유 식별 ID (자동 증가 PK) */
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	/** 연관된 예매 내역 정보 (외래키: reservations_id, 1:1 관계) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservations_id", nullable = false)
    private Reservation reservation;

    /** 결제 주체 유저 정보 (외래키: user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 외부 결제 API 제공 고유 번호 (TID) */
    @Column(nullable = false, length = 100)
    private String tid; // 카카오페이 고유 번호

    /** 가맹점에서 부여한 고유 주문 번호 */
    @Column(name = "partner_order_id", nullable = false, length = 100)
    private String partnerOrderId;

    /** 실제 최종 결제된 총 금액 */
    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    /** 결제 수단 정보 (예: MONEY, CARD 등) */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /** 결제 처리 상태 (READY, SUCCESS, FAILED, CANCELLED) */
    @Column(nullable = false, length = 20)
    private String status;
    
    /** 외부 결제 기관으로부터 전달받은 승인 시각 */
    private LocalDateTime approvedAt;
	
}
