package com.yonsai.backend.controller;

import com.yonsai.backend.dto.BookingRequestDto;
import com.yonsai.backend.service.ReservationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예매 결제 및 내역 관리를 위한 API 컨트롤러.
 *
 * @author ohseju
 * @since 2026-04-01
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/book")
    public ResponseEntity<?> bookSeats(@RequestBody BookingRequestDto requestDto) {
        try {
            reservationService.bookSeats(
                    requestDto.getUsername(),
                    requestDto.getShowtimeId(),
                    requestDto.getSelectedSeats(),
                    requestDto.getAdultCount(),
                    requestDto.getYouthCount()
            );
            return ResponseEntity.ok().body("{\"message\": \"예매가 완료되었습니다.\"}");
        } catch (IllegalStateException e) {
            log.warn("예매 실패 (Conflict): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"message\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            log.error("예매 처리 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"서버 오류가 발생했습니다.\"}");
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReservations(@RequestParam("username") String username) {
    
    	System.out.println(reservationService.getMyReservations(username));
    
        try {
            return ResponseEntity.ok(reservationService.getMyReservations(username));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"내역 조회 실패\"}");
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelReservation(@RequestBody Map<String, Object> payload) {
        try {
            Long reservationId = Long.valueOf(payload.get("reservationId").toString());
            List<Long> ticketIds = ((List<?>) payload.get("ticketIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .toList();
            int adultCancelCount = Integer.parseInt(payload.get("adultCancelCount").toString());
            int youthCancelCount = Integer.parseInt(payload.get("youthCancelCount").toString());
            String username = payload.get("username").toString();

            reservationService.cancelTickets(reservationId, ticketIds, adultCancelCount, youthCancelCount, username);
            return ResponseEntity.ok().body("{\"message\": \"취소가 완료되었습니다.\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            log.error("취소 처리 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"서버 오류\"}");
        }
    }
}
