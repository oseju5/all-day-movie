package com.yonsai.backend.service;

/**
 * 영화 데이터 처리를 위한 서비스 인터페이스.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
public interface MovieService {
    
    /**
     * KMDb API로부터 받은 JSON 응답을 파싱하여 DB에 저장/업데이트 합니다.
     * @param jsonResponse KMDb API의 JSON 형태 응답 문자열
     */
    void saveMoviesFromKmdb(String jsonResponse);
    String syncKmdbAndSave();

    /**
     * 메인 화면에 출력할 영화 목록을 조회하여 DTO로 반환합니다.
     * @return 영화 목록 DTO 리스트
     */
    java.util.List<com.yonsai.backend.dto.MovieResponseDto> getPopularMovies();

    /**
     * 오늘 업데이트된 영화를 선별하여 7일 뒤의 상영 회차 데이터를 생성합니다.
     */
    void generateShowtimesForUpdatedMovies();

    /**
     * 특정 영화와 특정 날짜의 상영 회차 정보를 조회합니다.
     */
    java.util.List<com.yonsai.backend.dto.BookingShowtimeDto> getBookingShowtimes(String movieId, String startDay);

    /**
     * 오늘부터 7일 후까지의 모든 영화에 대한 상영 회차 데이터를 생성합니다. (테스트용)
     */
    void generateAllShowtimes();

    /**
     * 30일이 지난 영화와 예매 내역을 삭제합니다.
     */
    void deleteOldData();

}
