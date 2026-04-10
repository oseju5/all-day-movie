package com.yonsai.backend.controller;

import com.yonsai.backend.dto.SeatResponseDto;
import com.yonsai.backend.service.SeatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예매 페이지(좌석 선택)를 위한 좌석 상태 API 컨트롤러.
 *
 * @author ohseju
 * @since 2026-04-01
 */
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/status")
    public ResponseEntity<List<SeatResponseDto>> getSeatsStatus(@RequestParam("showtimeId") Long showtimeId) {
        return ResponseEntity.ok(seatService.getSeatsWithStatus(showtimeId));
    }

}
