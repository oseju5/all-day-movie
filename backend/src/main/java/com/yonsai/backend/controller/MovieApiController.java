package com.yonsai.backend.controller;

import com.yonsai.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
public class MovieApiController {

    @Value("${KMDB_SERVICEKEY}") 
    private String serviceKey;

    private final MovieService movieService;

    @GetMapping("/api/movies")
    public java.util.List<com.yonsai.backend.dto.MovieResponseDto> getMovies() {
        return movieService.getPopularMovies();
    }

    @GetMapping("/api/movies/booking/showtimes")
    public java.util.List<com.yonsai.backend.dto.BookingShowtimeDto> getBookingShowtimes(
            @RequestParam("movieId") String movieId,
            @RequestParam("date") String date) {
        return movieService.getBookingShowtimes(movieId, date);
    }

    @GetMapping("/api/admin/kmdb")
    public String getKmdbMovies() {
        return movieService.syncKmdbAndSave();
    }

    @GetMapping("/api/admin/showtime")
    public String syncShowtimes() {
        movieService.syncKmdbAndSave();
        movieService.generateShowtimesForUpdatedMovies();
        return "KMDB 동기화 및 7일 뒤 회차 생성 완료";
    }

    @GetMapping("/api/admin/allshowtime")
    public String generateAllShowtimes() {
        movieService.syncKmdbAndSave();
        movieService.generateAllShowtimes();
        return "오늘부터 7일 후까지 모든 영화의 상영 회차 생성 완료";
    }
}
