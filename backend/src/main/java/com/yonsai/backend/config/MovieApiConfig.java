package com.yonsai.backend.config;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 통신을 위한 설정 클래스.
 * 
 * @author : ohseju
 * @since : 2026-03-17
 */
@Configuration
public class MovieApiConfig {

	/**
	 * RestTemplate 빈 등록. RestTemplateBuilder를 사용하면 타임아웃 등 세부 설정을 편하게 할 수 있습니다.
	 *
	 * @param builder 스프링이 자동으로 주입해주는 빌더
	 * @return 설정이 완료된 RestTemplate 객체
	 */
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		// 타임아웃 설정을 위한 새로운 표준 객체 생성
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(Duration.ofSeconds(5)) // 연결 타임아웃
				.withReadTimeout(Duration.ofSeconds(5)); // 읽기 타임아웃

		return builder.requestFactorySettings(settings) // 기존 방식 대신 이 메서드를 사용합니다.
				.build();
	}

}
