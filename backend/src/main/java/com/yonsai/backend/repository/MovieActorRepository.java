package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.MovieActor;

import com.yonsai.backend.entity.Movie;
import com.yonsai.backend.entity.Actor;

public interface MovieActorRepository extends JpaRepository<MovieActor, Long>{
    boolean existsByMovieAndActor(Movie movie, Actor actor);
}
