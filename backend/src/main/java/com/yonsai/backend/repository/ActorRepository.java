package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.Actor;

import java.util.Optional;

public interface ActorRepository extends JpaRepository<Actor, Long>{
    Optional<Actor> findByActorId(String actorId);
}
