package com.yonsai.backend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 마이페이지 조회를 위한 예매 내역 응답 DTO.
 * 엔티티 직접 반환 시 발생하는 Jackson 직렬화 오류(Hibernate Proxy)를 방지합니다.
 *
 * @author ohseju
 * @since 2026-04-01
 */
@Getter
@Builder
@ToString
public class ReservationResponseDto {
    private Long id;
    private String movieTitle;
    private String movieRating;
    private String startDay;
    private String startTime;
    private String screenName;
    private Integer adultCount;
    private Integer youthCount;
    private Integer totalPrice;
    private Integer cancelPrice;
    private String status;
    private List<TicketResponseDto> tickets;

    @Getter
    @Builder
    public static class TicketResponseDto {
        private Long id;
        private String seatNumber;
        private String status;
        private Integer price;
    }
}
