package com.yonsai.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yonsai.backend.entity.Payment;

public interface PaymentRepository 
					extends JpaRepository<Payment, Long>{

}
