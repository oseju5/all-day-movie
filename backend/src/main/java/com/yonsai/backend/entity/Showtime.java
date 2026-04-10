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
 * 특정 영화가 특정 상영관에서 상영되는 시간표 정보를 관리하는 엔티티.
 * 영화와 상영관 사이의 중간 매핑 정보를 담고 있습니다.
 *
 * @author : ohseju
 * @since : 2026-03-17
 */
@Entity
@Table(name = "Showtimes")
@Getter @Setter @NoArgsConstructor
public class Showtime extends BaseEntity{

	/** 고유 식별 ID (자동 증가 PK) */
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	/** 상영되는 영화 정보 (외래키: movie_id) */
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

	/** 영화가 상영되는 상영관 정보 (외래키: screen_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    /** 영화 상영 일자 (형식: YYYY-MM-DD) */
    @Column(name = "start_day", nullable = false, length = 15)
    private String startDay;
    
    /** 영화 시작 시간 (형식: HH:mm) */
    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;
	
    // Showtime이 삭제될 때 해당 회차로 예매된 Ticket들도 연쇄 삭제 (또는 Ticket에 외래키 ON DELETE CASCADE 설정 필요)
    @jakarta.persistence.OneToMany(mappedBy = "showtime", cascade = jakarta.persistence.CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<Ticket> tickets;

}
