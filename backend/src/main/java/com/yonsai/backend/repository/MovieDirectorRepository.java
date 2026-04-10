package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.MovieDirector;

import com.yonsai.backend.entity.Movie;
import com.yonsai.backend.entity.Director;

public interface MovieDirectorRepository extends JpaRepository<MovieDirector, Long>{
    boolean existsByMovieAndDirector(Movie movie, Director director);
}
