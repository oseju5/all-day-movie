package com.yonsai.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.Movie;

public interface MovieRepository 
					extends JpaRepository<Movie, String>{

    List<Movie> findByUpdatedAtBetween(LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT m.*, (embedding <=> cast(:queryVector as vector)) as distance " +
                "FROM movies m " +
                "WHERE embedding IS NOT NULL AND (embedding <=> cast(:queryVector as vector)) < 0.35 " +
                "ORDER BY distance ASC LIMIT 3", 
        nativeQuery = true
    )
    List<Object[]> findHighlySimilarMoviesWithDistance(@org.springframework.data.repository.query.Param("queryVector") String queryVector);

    // 네이티브 쿼리가 아닌 JPQL을 사용하여 엔티티 매핑 설정대로 안전하게 조인합니다.
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT m FROM Movie m " +
        "LEFT JOIN MovieActor ma ON m.docid = ma.movie.docid " +
        "LEFT JOIN Actor a ON ma.actor.actorId = a.actorId " +
        "LEFT JOIN MovieDirector md ON m.docid = md.movie.docid " +
        "LEFT JOIN Director d ON md.director.directorId = d.directorId " +
        "WHERE m.title LIKE %:keyword% " +
        "OR a.name LIKE %:keyword% " +
        "OR d.name LIKE %:keyword%"
    )
    List<Movie> findMoviesByObjectiveKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE movies SET embedding = cast(:vector as vector) WHERE docid = :docid", 
        nativeQuery = true
    )
    void updateMovieEmbedding(@org.springframework.data.repository.query.Param("docid") String docid, @org.springframework.data.repository.query.Param("vector") String vector);

}
