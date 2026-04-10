package com.yonsai.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yonsai.backend.entity.Movie;
import com.yonsai.backend.repository.MovieRepository;
import com.yonsai.backend.service.GeminiService;
import com.yonsai.backend.service.NaverCrawlingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

	private final GeminiService geminiService;
	private final NaverCrawlingService naverCrawlingService;
	private final MovieRepository movieRepository;

	// 프론트엔드 React 챗봇에서 직접 호출하기 위한 엔드포인트
	@PostMapping("/ask")
	public ResponseEntity<Map<String, Object>> askChatbot(@RequestBody Map<String, String> request) {
		String userQuery = request.get("query");
		if (userQuery == null || userQuery.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			// 1. 사용자 질문 분석 (주관적 vs 객관적)
			Map<String, String> analysis = geminiService.analyzeQuery(userQuery);
			String type = analysis.get("type"); // "GREETING", "REJECT", "TITLE", "PERSON" or "SUBJECTIVE"
			String keyword = analysis.get("keyword");

			// 1.5. 사전 필터링 (GREETING 또는 REJECT) 처리
			if ("GREETING".equalsIgnoreCase(type) || "REJECT".equalsIgnoreCase(type)) {
				log.info("사전 필터링(일상대화/거절) 응답 반환. 타입: {}, 메시지: {}", type, keyword);
				Map<String, Object> response = new HashMap<>();
				response.put("message", keyword != null ? keyword : "영화와 관련된 질문을 부탁드립니다.");
				response.put("cards", new ArrayList<>());
				return ResponseEntity.ok(response);
			}

			List<Movie> activeMovies = new ArrayList<>();

			// 2. DB 탐색
			if (("OBJECTIVE".equalsIgnoreCase(type) || "PERSON".equalsIgnoreCase(type)
					|| "TITLE".equalsIgnoreCase(type)) && keyword != null && !keyword.trim().isEmpty()) {
				log.info("객관적(인물/작품명) 검색 실행. 키워드: {}", keyword);
				org.springframework.data.domain.Pageable limit = org.springframework.data.domain.PageRequest.of(0, 3);
				activeMovies = movieRepository.findMoviesByObjectiveKeyword(keyword, limit);
			}

			// 만약 주관적(SUBJECTIVE) 질문인 경우에만 벡터 유사도 탐색을 수행합니다.
			if (activeMovies.isEmpty() && "SUBJECTIVE".equalsIgnoreCase(type)) {
				log.info("주관적(벡터) 검색 실행. 원본 질문: {}", userQuery);
				double[] vector = geminiService.getEmbedding(keyword != null ? keyword : userQuery);
				String vectorString = formatVector(vector);

				List<Object[]> results = movieRepository.findHighlySimilarMoviesWithDistance(vectorString);

				if (!results.isEmpty()) {
					// 보다 안전하게 마지막 컬럼값을 distance(거리)로 가져오기
					int lastIndex = results.get(0).length - 1;

					// DB에 임베딩이 비어있는(null) 레코드가 섞여서 반환되는 NPE 방어 (WHERE embedding IS NOT NULL 추가함)
					if (results.get(0)[lastIndex] != null) {
						Double topDistance = ((Number) results.get(0)[lastIndex]).doubleValue();

						log.info("벡터 검색 1위 영화 거리(Distance): {}", topDistance);

						// 거리가 짧을수록 좋음 (0에 가까울수록 완벽 일치). 엄격한 임계치 0.35로 설정.
						if (topDistance <= 0.35) {
							for (Object[] row : results) {
								if (row[lastIndex] != null) {
									Double distance = ((Number) row[lastIndex]).doubleValue();
									// 1위 결과가 기준을 통과했다면, 나머지 영화도 최대 0.35까지만 허용
									if (distance <= 0.35) {

										String foundDocid = null;

										for (Object col : row) {
											if (col == null)
												continue;

											String candidateId = String.valueOf(col).trim();
											if (candidateId.isEmpty() || candidateId.contains("http")
													|| candidateId.contains("-")) {
												continue;
											}
											Optional<Movie> movieOpt = movieRepository.findById(candidateId);

											if (movieOpt.isPresent()) {
												// DB에 존재한다면, 이 값이 진짜 docid이므로 리스트에 추가하고 루프 종료
												activeMovies.add(movieOpt.get());
												foundDocid = candidateId;
												break;
											}
										}

										// 루프를 다 돌았는데도 못 찾았을 경우에 대한 예외 처리 (선택 사항)
										if (foundDocid == null) {
											log.debug("해당 행(row)에서 유효한 영화 ID를 찾지 못했습니다.");
										}

									}
								}
							}
						}
					}
				}
			}
			List<Map<String, Object>> cards = new ArrayList<>();
			String botMessage = "요청하신 상황에 딱 맞는 추천 영화들입니다!";

			// 3. 상영작(DB 결과) 카드 추가
			if (!activeMovies.isEmpty()) {
				for (Movie m : activeMovies) {
					// 프론트엔드의 navigate state 활용을 위해 url 대신 movieId를 data 객체에 직접 전달
					cards.add(createFrontendCardWithId(m.getTitle(), m.getPosterUrl(), "예매하기", m.getDocid()));
				}
			} else {
				// 4. DB에 결과가 하나도 없다면 외부 OTT 영화 추천 진행
				log.info("DB 검색 결과 없음. OTT 외부 추천 실행.");
				botMessage = "현재 상영작 중에는 일치하는 내용이 없습니다. 대신 OTT 링크를 제안 드립니다!";

				List<String> suggestedTitles = new ArrayList<>();
				// 'TITLE' 타입이면 AI 추천 없이 바로 그 영화 제목으로 크롤링
				if ("TITLE".equalsIgnoreCase(type)) {
					suggestedTitles.add(keyword);
				} else {
					// 'PERSON' 또는 'SUBJECTIVE' 타입이면 Gemini에게 영화 추천을 받음
					suggestedTitles = geminiService.suggestMovies(userQuery);
				}

				for (String title : suggestedTitles) {
					com.yonsai.backend.dto.OttResultDto ottResult = naverCrawlingService.getOttLinkIfAvailable(title);
					if (ottResult != null && ottResult.getOttLink() != null) {
						cards.add(createFrontendCard(title, ottResult.getPosterUrl(), "보러가기", ottResult.getOttLink()));
					}
				}
			}

			if (cards.isEmpty()) {
				botMessage = "죄송합니다. 질문하신 검색 결과를 찾을 수 없습니다. 다른 질문을 입력해주시겠어요?";
			}

			// 프론트엔드 규격에 맞는 최종 응답 조립
			Map<String, Object> response = new HashMap<>();
			response.put("message", botMessage);
			response.put("cards", cards);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("React 챗봇 추천 처리 중 오류", e);
			return ResponseEntity.internalServerError().build();
		}
	}


	private String formatVector(double[] vector) {
		return "[" + Arrays.stream(vector).mapToObj(String::valueOf).collect(Collectors.joining(",")) + "]";
	}
	
	// 프론트엔드 React 연동용 카드 생성 헬퍼
	private Map<String, Object> createFrontendCard(String title, String imageUrl, String buttonName, String buttonUrl) {
		Map<String, Object> card = new HashMap<>();
		card.put("title", title);
		card.put("imageUrl", imageUrl);

		Map<String, Object> button = new HashMap<>();
		button.put("name", buttonName);
		button.put("data", Map.of("url", buttonUrl));

		card.put("buttons", List.of(button));
		return card;
	}

	// 예매하기 전용 카드 생성 헬퍼 (movieId 전달)
	private Map<String, Object> createFrontendCardWithId(String title, String imageUrl, String buttonName,
			String movieId) {
		Map<String, Object> card = new HashMap<>();
		card.put("title", title);
		card.put("imageUrl", imageUrl);

		Map<String, Object> button = new HashMap<>();
		button.put("name", buttonName);
		// 프론트엔드에서 navigate('/booking', { state: { movieId } }) 형식으로 쓰기 위함
		button.put("data", Map.of("movieId", movieId));

		card.put("buttons", List.of(button));
		return card;
	}
}
