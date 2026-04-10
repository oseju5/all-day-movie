package com.yonsai.backend.service;

import java.util.List;
import java.util.Map;

/**
 * AI 챗봇과 임베딩에 사용되는 제미나이 호출 서비스.
 *
 * @author ohseju
 * @since : 2026-03-27
 */
public interface GeminiService {
    double[] getEmbedding(String text);
    String extractKeywordsFromPlot(String plot);
    Map<String, String> analyzeQuery(String userQuery);
    List<String> suggestMovies(String userQuery);
}
