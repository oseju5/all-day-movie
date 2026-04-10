package com.yonsai.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yonsai.backend.entity.MovieMetadata;

@Repository
public interface MovieMetadataRepository extends JpaRepository<MovieMetadata, String> {
    
    // 상태가 false(유효하지 않은 링크)인 데이터를 조회 (배치 작업용)
    List<MovieMetadata> findByIsValidFalse();
}
