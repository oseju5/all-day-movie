package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.Screen;

public interface ScreenRepository 
					extends JpaRepository<Screen, Long>{

}
