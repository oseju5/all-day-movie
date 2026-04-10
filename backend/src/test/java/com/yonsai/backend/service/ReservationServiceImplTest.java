package com.yonsai.backend.service;

import com.yonsai.backend.service.impl.ReservationServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yonsai.backend.entity.Reservation;
import com.yonsai.backend.entity.Screen;
import com.yonsai.backend.entity.Showtime;
import com.yonsai.backend.entity.Ticket;
import com.yonsai.backend.entity.User;
import com.yonsai.backend.repository.ReservationRepository;
import com.yonsai.backend.repository.ShowtimeRepository;
import com.yonsai.backend.repository.TicketRepository;
import com.yonsai.backend.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReservationServiceImpl 비즈니스 로직 테스트 (부분 취소 및 시간 초과 중점)
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock; // 테스트 시나리오에 따라 시간을 조작하기 위해 Mock 주입

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User testUser;
    private Showtime testShowtime;
    private Screen testScreen;
    private Reservation testReservation;
    private Ticket ticket1;
    private Ticket ticket2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testScreen = new Screen();
        testScreen.setId(1L);
        testScreen.setPriceAdult(15000);
        testScreen.setPriceChild(12000);

        testShowtime = new Showtime();
        testShowtime.setId(10L);
        testShowtime.setScreen(testScreen);
        testShowtime.setStartDay("2026-04-10");
        testShowtime.setStartTime("15:00"); // 상영 시작 시간: 15시 정각

        testReservation = new Reservation();
        testReservation.setId(100L);
        testReservation.setUser(testUser);
        testReservation.setShowtime(testShowtime);
        testReservation.setAdultCount(2);
        testReservation.setYouthCount(0);
        testReservation.setTotalPrice(30000);
        testReservation.setCancelPrice(0);
        testReservation.setStatus("COMPLETED");

        ticket1 = new Ticket();
        ticket1.setId(1001L);
        ticket1.setSeatNumber("A1");
        ticket1.setStatus("ACTIVE");
        ticket1.setPrice(15000);
        ticket1.setReservation(testReservation);

        ticket2 = new Ticket();
        ticket2.setId(1002L);
        ticket2.setSeatNumber("A2");
        ticket2.setStatus("ACTIVE");
        ticket2.setPrice(15000);
        ticket2.setReservation(testReservation);
    }

    @Test
    @DisplayName("상영 20분 전(여유 있음) - 티켓 1장 부분 취소 성공")
    void cancelTickets_success_partialCancel() {
        // Given
        // 1. 현재 시간을 "2026-04-10 14:00"으로 설정 (상영 1시간 전)
        String mockNowStr = "2026-04-10T14:00:00Z"; // Z는 UTC 기준 (Instant 변환용)
        Clock fixedClock = Clock.fixed(Instant.parse(mockNowStr), ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));
        when(ticketRepository.findByReservation(testReservation)).thenReturn(Arrays.asList(ticket1, ticket2));

        // When
        // 티켓 1장(ticket1) 취소 요청 (성인 1명)
        reservationService.cancelTickets(100L, Arrays.asList(1001L), 1, 0, "testuser");

        // Then
        // 1. 선택된 티켓의 상태가 CANCELLED로 변경되었는지 확인
        assertThat(ticket1.getStatus()).isEqualTo("CANCELLED");
        // 2. 선택되지 않은 티켓은 여전히 ACTIVE인지 확인
        assertThat(ticket2.getStatus()).isEqualTo("ACTIVE");
        
        // 3. Reservation의 금액 및 인원 변경 확인 (30000원 중 15000원 환불)
        assertThat(testReservation.getCancelPrice()).isEqualTo(15000);
        assertThat(testReservation.getAdultCount()).isEqualTo(1); // 2명 -> 1명
        assertThat(testReservation.getStatus()).isEqualTo("PARTIAL_CANCELLED");

        verify(reservationRepository).save(testReservation);
    }

    @Test
    @DisplayName("상영 20분 미만 남음 - 예매 취소 불가 (IllegalStateException 발생)")
    void cancelTickets_fail_timeLimitExceeded() {
        // Given
        // 1. 현재 시간을 "2026-04-10 14:41"로 설정 (상영 19분 전)
        String mockNowStr = "2026-04-10T14:41:00Z";
        Clock fixedClock = Clock.fixed(Instant.parse(mockNowStr), ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));

        // When & Then
        // 예외가 터져야 성공!
        assertThrows(IllegalStateException.class, () -> {
            reservationService.cancelTickets(100L, Arrays.asList(1001L, 1002L), 2, 0, "testuser");
        });
    }

    @Test
    @DisplayName("정상 예매 성공 - 좌석 2개 예매 시 정상 저장 여부 검증")
    void bookSeats_success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(showtimeRepository.findById(10L)).thenReturn(Optional.of(testShowtime));
        
        Reservation expectedReservation = new Reservation();
        expectedReservation.setId(200L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(expectedReservation);

        // When
        Reservation result = reservationService.bookSeats("testuser", 10L, Arrays.asList("B1", "B2"), 2, 0);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L);
        // Reservation 저장이 1번, Ticket 저장이 2번 호출되어야 함
        verify(reservationRepository).save(any(Reservation.class));
        verify(ticketRepository, times(2)).save(any(Ticket.class)); // B1, B2에 대해 총 2회 호출됨
    }

    @Test
    @DisplayName("중복 예매 방지(동시성 충돌) - DataIntegrityViolationException 발생 시 IllegalStateException으로 변환")
    void bookSeats_fail_concurrency() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(showtimeRepository.findById(10L)).thenReturn(Optional.of(testShowtime));
        
        Reservation expectedReservation = new Reservation();
        when(reservationRepository.save(any(Reservation.class))).thenReturn(expectedReservation);
        
        // Ticket 저장 시 DataIntegrityViolationException 예외 던지도록 mock (중복 예매 상황 시뮬레이션)
        when(ticketRepository.save(any(Ticket.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException("Unique index violation"));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reservationService.bookSeats("testuser", 10L, Arrays.asList("A1"), 1, 0);
        });
        
        assertThat(exception.getMessage()).contains("이미 예매된 좌석입니다");
    }

    @Test
    @DisplayName("전체 취소 성공 - 모든 티켓 취소 시 Reservation 상태 ALL_CANCELLED로 변경")
    void cancelTickets_success_allCancel() {
        // Given
        String mockNowStr = "2026-04-10T12:00:00Z"; // 충분히 여유 있는 시간
        Clock fixedClock = Clock.fixed(Instant.parse(mockNowStr), ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));
        when(ticketRepository.findByReservation(testReservation)).thenReturn(Arrays.asList(ticket1, ticket2));

        // When (모든 티켓인 1001, 1002를 취소)
        reservationService.cancelTickets(100L, Arrays.asList(1001L, 1002L), 2, 0, "testuser");

        // Then
        assertThat(ticket1.getStatus()).isEqualTo("CANCELLED");
        assertThat(ticket2.getStatus()).isEqualTo("CANCELLED");
        
        // 성인 2명이 취소되었으므로 성인 카운트는 0, 가격은 30000원이 환불되어야 함
        assertThat(testReservation.getCancelPrice()).isEqualTo(30000);
        assertThat(testReservation.getAdultCount()).isEqualTo(0);
        assertThat(testReservation.getStatus()).isEqualTo("ALL_CANCELLED");
        
        verify(reservationRepository).save(testReservation);
    }

    @Test
    @DisplayName("타인 예매 취소 시도 - 권한 없음 (SecurityException 발생)")
    void cancelTickets_fail_unauthorized() {
        // Given
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));
        
        // When & Then
        // testuser가 아닌 hacker가 취소하려고 할 때
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            reservationService.cancelTickets(100L, Arrays.asList(1001L), 1, 0, "hacker");
        });
        
        assertThat(exception.getMessage()).contains("본인의 예매 내역만 취소할 수 있습니다");
    }
}
