package com.yonsai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배우 정보를 저장하는 엔티티.
 * 
 * * @author ohseju
 * @since 2026-03-18
 */

@Entity
@Table(name = "actors")
@Getter @Setter @NoArgsConstructor
public class Actor extends BaseEntity{
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** KMDb 배우 고유 ID */
    @Column(name = "actor_id", length = 20, unique = true)
    private String actorId;

    /** 배우 국문명 */
    @Column(nullable = false, length = 100)
    private String name;


}
