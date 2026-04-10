package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 영화가 상영되는 상영관의 정보를 관리하는 엔티티.
 * 상영관별 좌석 수 및 연령별 요금 정책을 포함합니다.
 *
 * @author : ohseju
 * @since : 2026-03-17
 */
@Entity
@Table(name = "screens")
@Getter @Setter @NoArgsConstructor
public class Screen extends BaseEntity{
	
	/** 고유 식별 ID (자동 증가 PK) */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	/** 상영관 이름 (예: 1관, 2관, IMAX관 등) */
	@Column(nullable = false, length = 50)
    private String name;
	
	/** 해당 상영관의 총 좌석 수 */
    @Column(name = "total_seat", nullable = false)
    private Integer totalSeat;
    
    /** 청소년 권종 기본 요금 */
    @Column(name = "price_child", nullable = false)
    private Integer priceChild;
    
    /** 성인 권종 기본 요금 */
    @Column(name = "price_adult", nullable = false)
    private Integer priceAdult;
	
}
