package com.yonsai.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.yonsai.backend.entity.Movie;
import com.yonsai.backend.entity.Showtime;

public interface ShowtimeRepository 
					extends JpaRepository<Showtime, Long>{

    List<Showtime> findByMovieAndStartDayOrderByStartTimeAsc(Movie movie, String startDay);

    /**
     * 특정 날짜 이후(포함)에 상영 회차가 있는 모든 영화를 중복 없이 조회합니다.
     * @param today 오늘 날짜 (YYYY-MM-DD)
     * @return 영화 엔티티 리스트
     */
    @Query("SELECT DISTINCT s.movie FROM Showtime s WHERE s.startDay >= :today")
    List<Movie> findMoviesWithShowtimesAfter(@Param("today") String today);

}
