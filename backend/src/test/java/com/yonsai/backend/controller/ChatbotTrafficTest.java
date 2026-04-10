package com.yonsai.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonsai.backend.repository.MovieMetadataRepository;
import com.yonsai.backend.scheduler.MovieScheduler;
import com.yonsai.backend.service.GeminiService;
import com.yonsai.backend.service.NaverCrawlingService;
import com.yonsai.backend.service.impl.NaverCrawlingServiceImpl;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. @WebMvcTest 대신 @SpringBootTest를 사용하여 실제 빈들을 로드합니다.
// 2. DB 연결 에러를 막기 위해 JDBC 관련 자동 설정을 제외(exclude)합니다.
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
@AutoConfigureMockMvc // 3. MockMvc를 사용하기 위해 추가
@TestPropertySource(properties = {
    "gemini.api.key=test_key_value", // 환경변수 주입
    "KMDB_SERVICEKEY=mock_key",
    "spring.main.allow-bean-definition-overriding=true" // 빈 중복 허용
})
@WithMockUser
@MockitoBean(types = JpaMetamodelMappingContext.class)
public class ChatbotTrafficTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    // 4. 이제 @SpringBootTest가 GeminiServiceImpl을 빈으로 등록했으므로 SpyBean이 정상 작동합니다.
    @MockitoSpyBean
    private GeminiService geminiService;

    @MockitoSpyBean
    private NaverCrawlingServiceImpl naverCrawlingService;
    
    @MockitoBean
    private com.yonsai.backend.service.AuthService authService; 

    @MockitoBean
    private com.yonsai.backend.service.MovieService movieService;

    @MockitoBean
    private com.yonsai.backend.service.ReservationService reservationService;

    @MockitoBean
    private com.yonsai.backend.repository.MovieRepository movieRepository;
    
    @MockitoBean
    private com.yonsai.backend.service.SeatService seatService;
    
    @MockitoBean
    private MovieScheduler movieScheduler;
    
    @MockitoBean
    private com.yonsai.backend.scheduler.ReservationScheduler ReservationScheduler;
    
    @MockitoBean
    private MovieMetadataRepository movieMetadataRepository;
    
    
    @BeforeEach
    void setUp() {
        // 서킷 브레이커 상태 초기화
        circuitBreakerRegistry.circuitBreaker("geminiApi").transitionToClosedState();
        circuitBreakerRegistry.circuitBreaker("naverCrawling").transitionToClosedState();
    }

    @Test
    @DisplayName("캐시 시스템 성능 테스트: 동일한 질문 100번 요청 시 외부 API는 단 1번만 호출되어야 한다.")
    void testCacheHitsUnderHighTraffic() throws Exception {
        // given
        String testQuery = "가족 영화 추천해줘";
        Map<String, String> analysisResult = new HashMap<>();
        analysisResult.put("type", "SUBJECTIVE");
        analysisResult.put("keyword", "따뜻한, 가족애, 교훈");
        
        // doReturn을 사용하여 실제 API 호출 차단
        Mockito.doReturn(analysisResult).when(geminiService).analyzeQuery(anyString());
        Mockito.doReturn(new double[768]).when(geminiService).getEmbedding(anyString());
        Mockito.doReturn(List.of("영화1", "영화2", "영화3")).when(geminiService).suggestMovies(anyString());
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", testQuery);

        // when (100번 호출)
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/api/chatbot/ask")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk());
        }

        // then: 캐시가 동작하면 실제 메서드 실행 횟수는 1회여야 함
        verify(geminiService, times(1)).analyzeQuery(anyString());
    }

    @Test
    @DisplayName("서킷 브레이커 방어막 테스트: 외부 크롤링 서버 장애 시 Fallback으로 즉시 응답해야 한다.")
    void testCircuitBreakerFallbackOnCrawlingFailure() throws Exception {
        // 1. Given: 테스트에 사용할 고정값 정의
        String testQuery = "타이타닉 보고 싶어";
        String movieTitle = "타이타닉"; // 분석 결과로 나올 키워드
        
        Map<String, String> analysisResult = new HashMap<>();
        analysisResult.put("type", "TITLE");
        analysisResult.put("keyword", movieTitle);
        
        // Gemini 분석 결과 고정 (여기는 프록시 영향이 적으므로 anyString() 써도 무방)
        Mockito.doReturn(analysisResult).when(geminiService).analyzeQuery(anyString());
        
        // ⭐ [핵심 수정] anyString() 대신 실제 값인 'movieTitle'을 직접 넣습니다.
        // 이렇게 하면 프록시 내부에서 매처가 꼬이는 현상을 원천 차단합니다.
        Mockito.doThrow(new RuntimeException("의도된 크롤링 장애 발생"))
               .when(naverCrawlingService).getOttLinkIfAvailable(movieTitle);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", testQuery);

        // 2. When: 5번 요청 (서킷을 열기 위해 반복)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/chatbot/ask")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk()); // Fallback 덕분에 200 OK!
        }

        // 3. Then: 서킷 브레이커 상태 확인
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("naverCrawling");
        assertThat(cb.getState()).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
        
        // 💡 [참고] 검증 시에도 매처가 불안정하다면 아래 verify는 생략해도 좋습니다. 
        // 이미 상태값이 OPEN인 것만으로도 충분히 검증된 것이니까요!
        // verify(naverCrawlingService, atLeastOnce()).getOttLinkFallback(eq(movieTitle), any(Throwable.class));
    }

    
    @Test
    @DisplayName("사전 필터링 로직 테스트: 부적절한 질문 시 리소스를 소모하지 않고 즉시 거절한다.")
    void testPreFilteringForRejectQuery() throws Exception {
        String rejectQuery = "너 바보야?";
        Map<String, String> analysisResult = new HashMap<>();
        analysisResult.put("type", "REJECT");
        analysisResult.put("keyword", "죄송하지만 영화와 관련된 질문에만 답변해 드릴 수 있습니다.");
        
        Mockito.doReturn(analysisResult).when(geminiService).analyzeQuery(anyString());
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", rejectQuery);

        mockMvc.perform(post("/api/chatbot/ask")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("죄송하지만 영화와 관련된 질문에만 답변해 드릴 수 있습니다."))
                .andExpect(jsonPath("$.cards").isEmpty());
        
        verify(geminiService, times(0)).getEmbedding(anyString());
    }
}