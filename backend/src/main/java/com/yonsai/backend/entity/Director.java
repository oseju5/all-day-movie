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
 * 감독 정보를 저장하는 엔티티.
 * 
 * * @author ohseju
 * @since 2026-03-18
 */

@Entity
@Table(name = "directors")
@Getter @Setter @NoArgsConstructor
public class Director extends BaseEntity{

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** KMDb 감독 고유 ID */
    @Column(name = "director_id", length = 20, unique = true)
    private String directorId;

    /** 감독 국문명 */
    @Column(nullable = false, length = 100)
    private String name;

}
