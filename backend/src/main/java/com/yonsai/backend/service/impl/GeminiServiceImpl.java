package com.yonsai.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonsai.backend.service.GeminiService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Cacheable(value = "embeddings", key = "#p0")
    @CircuitBreaker(name = "geminiApi", fallbackMethod = "getEmbeddingFallback")
    public double[] getEmbedding(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + geminiApiKey;

        Map<String, Object> part = new HashMap<>();
        part.put("text", text);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> request = new HashMap<>();
        request.put("content", content);
        request.put("outputDimensionality", 768);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("embedding")) {
                Map<String, Object> embedding = (Map<String, Object>) body.get("embedding");
                List<Double> values = (List<Double>) embedding.get("values");

                double[] result = new double[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    result[i] = values.get(i);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Gemini Embedding API 호출 실패", e);
            throw new RuntimeException("Embedding API 실패", e); // CircuitBreaker가 예외를 감지할 수 있도록 다시 던짐
        }
        return new double[768];
    }

    // CircuitBreaker Fallback 메서드 (예외 파라미터가 있어야 매칭됨)
    public double[] getEmbeddingFallback(String text, Throwable t) {
        log.warn("Circuit Breaker 동작: 임베딩 서버 과부하 또는 오류로 빈 배열 반환. ({})", t.getMessage());
        return new double[768];
    }

    @Override
    @Cacheable(value = "extractedKeywords", key = "#p0")
    @CircuitBreaker(name = "geminiApi", fallbackMethod = "extractKeywordsFallback")
    public String extractKeywordsFromPlot(String plot) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        String prompt = "너는 영화 전문가야. 다음 영화 줄거리를 읽고, 이 영화의 '핵심 장르', '전체적인 분위기', '주요 테마나 감정'을 나타내는 키워드를 5~7개만 추출해서 쉼표(,)로 구분해 반환해.\n" +
            "예를 들어 공포 영화라면 '공포, 호러, 스릴러, 기괴한, 미스터리' 와 같이 장르적 키워드가 반드시 포함되어야 해.\n" +
            "부가 설명이나 번호, 특수기호 없이 오직 단어들만 쉼표로 이어붙여서 출력해.\n\n" +
            "줄거리: " + plot;

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                    if (!parts.isEmpty()) {
                        String text = (String) parts.get(0).get("text");
                        if (text != null && !text.isEmpty()) {
                            return text.trim().replaceAll("[\"'\n\r]", "");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini 영화 줄거리 키워드 추출 실패", e);
            throw new RuntimeException("Keyword Extraction 실패", e);
        }
        return ""; 
    }

    public String extractKeywordsFallback(String plot, Throwable t) {
        log.warn("Circuit Breaker 동작: 키워드 추출 서버 과부하로 원본 줄거리만 사용. ({})", t.getMessage());
        return "";
    }

    @Override
    @Cacheable(value = "analyzedQueries", key = "#p0")
    @CircuitBreaker(name = "geminiApi", fallbackMethod = "analyzeQueryFallback")
    public Map<String, String> analyzeQuery(String userQuery) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        String prompt = "너는 영화 추천 AI 데이터 엔지니어야. 사용자의 요청: '" + userQuery + "'\n" +
            "아래의 기준에 따라 요청을 분류하고 그에 맞는 JSON 응답을 줘.\n\n" +
            "[AI 채팅 사전 필터링]\n" +
            "A. 사용자가 일상 대화나 인사(안녕, 반가워, 누구니? 등)를 입력한 경우 'GREETING'으로 분류해. 'keyword' 필드에는 친절하게 인사를 받아주고 영화 서비스임을 상기시키며 영화 질문을 유도하는 답변을 적어줘.\n" +
            "B. 사용자가 부적절하거나 범위 밖(금융, 정치, 비속어, 날씨 등)의 문장을 입력한 경우 'REJECT'로 분류해. 'keyword' 필드에는 \"죄송하지만 영화와 관련된 질문에만 답변해 드릴 수 있습니다.\"라고 정중히 거절하는 답변을 적어줘.\n\n" +
            "[영화 검색 필터링]\n" +
            "1. 이 요청이 특정 '영화 제목'을 정확하게 지목하여 보길 원하면 'TITLE'로 분류하고, 오직 그 영화의 제목 하나만 'keyword'에 담아줘 (예: '타이타닉 보고 싶어' -> type: TITLE, keyword: '타이타닉').\n" +
            "2. 이 요청이 '감독명', '배우명'을 지목하여 그 사람이 참여한 영화들을 찾고 싶어한다면 'PERSON'으로 분류하고, 오직 그 사람 이름 하나만 'keyword'에 담아줘 (예: '봉준호 감독 작품' -> type: PERSON, keyword: '봉준호').\n" +
            "3. '애니메이션', '로봇', '가족', '우울할 때' 등 분위기, 장르, 감성과 관련된 주관적 검색이면 'SUBJECTIVE'로 분류해.\n" +
            "4. [중요] 만약 'SUBJECTIVE'로 분류되었다면, 사용자의 질문을 영화 줄거리에 자주 등장할 법한 연관 키워드, 감정 단어 10~15개로 쉼표로 연결해서 'keyword' 필드에 확장해줘 (예: '로봇 영화' -> 'SF, 미래, 인공지능, 안드로이드...').\n\n" +
            "결과는 반드시 아래 JSON 형식으로만 대답해. 부가 설명이나 markdown 기호(```)는 절대 쓰지 마.\n" +
            "{\"type\": \"GREETING, REJECT, TITLE, PERSON, SUBJECTIVE 중 하나\", \"keyword\": \"추출된 키워드 또는 답변 메시지\"}";

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));

        Map<String, String> result = new HashMap<>();
        result.put("type", "SUBJECTIVE");
        result.put("keyword", userQuery);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                    if (!parts.isEmpty()) {
                        String text = (String) parts.get(0).get("text");
                        if (text != null) {
                            text = text.replaceAll("```json", "").replaceAll("```", "").trim();
                            ObjectMapper mapper = new ObjectMapper();
                            try {
                                JsonNode jsonNode = mapper.readTree(text);
                                result.put("type", jsonNode.path("type").asText("SUBJECTIVE"));
                                result.put("keyword", jsonNode.path("keyword").asText(userQuery));
                            } catch (Exception parseEx) {
                                log.error("JSON 파싱 실패", parseEx);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini Analyze Query 실패", e);
            throw new RuntimeException("Analyze Query 실패", e);
        }
        return result;
    }

    public Map<String, String> analyzeQueryFallback(String userQuery, Throwable t) {
        log.warn("Circuit Breaker 동작: 쿼리 분석 서버 과부하. 임시 메시지 반환. ({})", t.getMessage());
        Map<String, String> fallbackResult = new HashMap<>();
        fallbackResult.put("type", "GREETING"); // 컨트롤러가 빈 배열 리턴하도록 우회
        fallbackResult.put("keyword", "현재 많은 분이 서비스를 이용 중이어서 AI 분석에 시간이 조금 더 걸리고 있습니다. 잠시 후 다시 시도해 주세요.");
        return fallbackResult;
    }

    @Override
    @Cacheable(value = "suggestedMovies", key = "#p0")
    @CircuitBreaker(name = "geminiApi", fallbackMethod = "suggestMoviesFallback")
    public List<String> suggestMovies(String userQuery) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        String prompt = "사용자의 요청: '" + userQuery + "'. 이 상황에 어울리는 유명한 상업 영화 3개의 제목만 콤마(,)로 구분해서 반환해줘. 부가 설명이나 번호, 특수기호 없이 오직 영화 제목만 콤마로 이어붙여서 출력해. (예: 인셉션, 어바웃 타임, 위대한 쇼맨)";

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));

        List<String> suggestedTitles = new ArrayList<>();
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                    if (!parts.isEmpty()) {
                        String text = (String) parts.get(0).get("text");
                        if (text != null) {
                            String[] titles = text.split(",");
                            for (String title : titles) {
                                String cleanTitle = title.trim().replaceAll("[\"'\n\r]", "");
                                if (!cleanTitle.isEmpty()) {
                                    suggestedTitles.add(cleanTitle);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini GenerateContent API 호출 실패", e);
            throw new RuntimeException("Suggest Movies 실패", e);
        }
        return suggestedTitles;
    }

    public List<String> suggestMoviesFallback(String userQuery, Throwable t) {
        log.warn("Circuit Breaker 동작: OTT 영화 추천 서버 과부하. 추천 생략. ({})", t.getMessage());
        return new ArrayList<>();
    }
}
