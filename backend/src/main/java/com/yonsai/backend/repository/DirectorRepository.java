package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.Director;

import java.util.Optional;

public interface DirectorRepository extends JpaRepository<Director, Long>{
    Optional<Director> findByDirectorId(String directorId);
}
