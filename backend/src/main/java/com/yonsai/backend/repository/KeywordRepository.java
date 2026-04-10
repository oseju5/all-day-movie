package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.Keyword;

import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long>{
    Optional<Keyword> findByKeywords(String keywords);
}
