package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.MovieKeyword;

import com.yonsai.backend.entity.Movie;
import com.yonsai.backend.entity.Keyword;

public interface MovieKeywordRepository extends JpaRepository<MovieKeyword, Long>{
    boolean existsByMovieAndKeyword(Movie movie, Keyword keyword);
}
