package com.yonsai.backend.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 프론트엔드에서 전송하는 예매 결제 요청 DTO.
 *
 * @author ohseju
 * @since 2026-04-01
 */
@Getter
@Setter
public class BookingRequestDto {
    private String username;
    private Long showtimeId;
    private List<String> selectedSeats;
    private int adultCount;
    private int youthCount;
}
