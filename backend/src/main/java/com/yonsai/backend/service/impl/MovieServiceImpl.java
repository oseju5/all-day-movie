package com.yonsai.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonsai.backend.entity.*;
import com.yonsai.backend.repository.*;
import com.yonsai.backend.service.GeminiService;
import com.yonsai.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 영화 데이터 처리를 위한 서비스 구현체.
 *
 * @author ohseju
 * @since : 2026-03-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovieServiceImpl implements MovieService {

    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final KeywordRepository keywordRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieDirectorRepository movieDirectorRepository;
    private final MovieKeywordRepository movieKeywordRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ScreenRepository screenRepository;
    private final TicketRepository ticketRepository;
    private final GeminiService geminiService;
    private final ReservationRepository reservationRepository;

    @org.springframework.beans.factory.annotation.Value("${KMDB_SERVICEKEY}") 
    private String serviceKey;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.yonsai.backend.dto.BookingShowtimeDto> getBookingShowtimes(String movieId, String startDay) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영화입니다."));

        java.util.List<Showtime> showtimes = showtimeRepository.findByMovieAndStartDayOrderByStartTimeAsc(movie, startDay);

        return showtimes.stream().map(st -> {
            int reservedCount = ticketRepository.countActiveTicketsByShowtimeId(st.getId());
            int totalSeats = st.getScreen().getTotalSeat();
            
            // 종료 시간 계산 (시작 시간 + 런타임)
            String endTime = calculateEndTime(st.getStartTime(), movie.getRuntime());

            return com.yonsai.backend.dto.BookingShowtimeDto.builder()
                    .id(st.getId())
                    .startTime(st.getStartTime())
                    .endTime(endTime)
                    .totalSeats(totalSeats)
                    .remainingSeats(totalSeats - reservedCount)
                    .screenName(st.getScreen().getName())
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    private String calculateEndTime(String startTime, Integer runtime) {
        if (runtime == null || runtime <= 0) runtime = 120; // 기본 런타임 120분 설정
        try {
            java.time.LocalTime start = java.time.LocalTime.parse(startTime);
            java.time.LocalTime end = start.plusMinutes(runtime);
            return end.toString();
        } catch (Exception e) {
            return startTime;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.yonsai.backend.dto.MovieResponseDto> getPopularMovies() {
        // 오늘을 포함한 이후 날짜에 상영 회차가 있는 영화만 가져옵니다.
        String today = java.time.LocalDate.now().toString();
        java.util.List<Movie> movies = showtimeRepository.findMoviesWithShowtimesAfter(today);
        
        return movies.stream().map(m -> {
            com.yonsai.backend.dto.MovieResponseDto dto = new com.yonsai.backend.dto.MovieResponseDto();
            dto.setId(m.getDocid());
            dto.setTitle(m.getTitle());
            dto.setPoster(m.getPosterUrl());
            dto.setRating(m.getRating()); 
            dto.setRuntime(m.getRuntime() != null && m.getRuntime() > 0 ? m.getRuntime() : 120); 
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public void generateShowtimesForUpdatedMovies() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // 1. 오늘 업데이트된 영화 조회
        java.util.List<Movie> updatedMovies = movieRepository.findByUpdatedAtBetween(startOfDay, endOfDay);
        log.info("오늘 업데이트된 영화 개수: {}", updatedMovies.size());

        if (updatedMovies.isEmpty()) {
            return;
        }

        // 2. 7일 뒤 날짜 계산
        java.time.LocalDate targetDate = today.plusDays(7);
        String startDay = targetDate.toString(); // YYYY-MM-DD

        // 3. 상영관 정보 조회 (1~8관)
        java.util.List<Screen> screens = screenRepository.findAll();
        if (screens.isEmpty()) {
            log.warn("등록된 상영관이 없습니다. 회차를 생성할 수 없습니다.");
            return;
        }

        String[] startTimes = {"12:00", "15:00", "18:00", "21:00"};
        int screenIndex = 0;

        // 4. 각 영화별 회차 생성
        for (Movie movie : updatedMovies) {
            for (String startTime : startTimes) {
                Showtime showtime = new Showtime();
                showtime.setMovie(movie);
                // 상영관을 순차적으로 배정 (1~8관 순환)
                showtime.setScreen(screens.get(screenIndex % screens.size()));
                showtime.setStartDay(startDay);
                showtime.setStartTime(startTime);
                
                showtimeRepository.save(showtime);
                screenIndex++;
            }
        }
        log.info("총 {}개의 회차 데이터 생성이 완료되었습니다. (대상 날짜: {})", updatedMovies.size() * 4, startDay);
    }

    @Override
    @Transactional
    public void generateAllShowtimes() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<Movie> allMovies = movieRepository.findAll();
        java.util.List<Screen> screens = screenRepository.findAll();

        if (allMovies.isEmpty() || screens.isEmpty()) {
            log.warn("영화 또는 상영관 정보가 없어 회차를 생성할 수 없습니다.");
            return;
        }

        String[] startTimes = {"12:00", "15:00", "18:00", "21:00"};
        int totalCreated = 0;

        for (int i = 0; i <= 7; i++) {
            String targetDay = today.plusDays(i).toString();
            int screenIndex = 0;
            
            for (Movie movie : allMovies) {
                for (String startTime : startTimes) {
                    Showtime showtime = new Showtime();
                    showtime.setMovie(movie);
                    showtime.setScreen(screens.get(screenIndex % screens.size()));
                    showtime.setStartDay(targetDay);
                    showtime.setStartTime(startTime);
                    
                    showtimeRepository.save(showtime);
                    screenIndex++;
                    totalCreated++;
                }
            }
        }
        log.info("전체 영화 대상 8일간의 회차 데이터 {}개가 생성되었습니다.", totalCreated);
    }

    @Override
    @Transactional
    public void deleteOldData() {
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);

        try {
            // 1. 30일 지난 예약 건 삭제
            java.util.List<Reservation> oldReservations = reservationRepository.findAll().stream()
                .filter(res -> res.getCreatedAt() != null && res.getCreatedAt().isBefore(thirtyDaysAgo))
                .collect(Collectors.toList());
            
            if (!oldReservations.isEmpty()) {
                reservationRepository.deleteAll(oldReservations);
                log.info("30일 경과된 예약 데이터 {}건 삭제 완료.", oldReservations.size());
            }

            // 2. 30일 지난 영화 건 삭제
            java.util.List<Movie> oldMovies = movieRepository.findAll().stream()
                .filter(m -> m.getUpdatedAt() != null && m.getUpdatedAt().isBefore(thirtyDaysAgo))
                .collect(Collectors.toList());

            if (!oldMovies.isEmpty()) {
                movieRepository.deleteAll(oldMovies);
                log.info("업데이트된 지 30일 경과된 영화 데이터 {}건 삭제 완료.", oldMovies.size());
            }

        } catch (Exception e) {
            log.error("오래된 데이터 삭제 중 오류 발생", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEmbeddingSafe(String docid, String title, String textForVector) {
        try {
            double[] vector = geminiService.getEmbedding(textForVector);
            String vectorString = "[" + Arrays.stream(vector).mapToObj(String::valueOf).collect(Collectors.joining(",")) + "]";
            movieRepository.updateMovieEmbedding(docid, vectorString);
            log.info("영화 임베딩 업데이트 완료: {}", title);
        } catch (Exception e) {
            log.error("임베딩 생성/저장 실패 (DB 저장은 유지됨): {}", title, e);
        }
    }

    // 프록시 주입 (자기 자신 호출 시 REQUIRES_NEW 적용 위함)
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private MovieServiceImpl selfProxy;

    private void updateEmbeddingSafeProxyCall(String docid, String title, String textForVector) {
        if (selfProxy != null) {
            selfProxy.updateEmbeddingSafe(docid, title, textForVector);
        } else {
            updateEmbeddingSafe(docid, title, textForVector);
        }
    }

    @Override
    public String syncKmdbAndSave() {
        String baseUrl = "http://api.koreafilm.or.kr/openapi-data2/wisenut/search_api/search_json2.jsp";
        
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate oneMonthAgo = now.minusMonths(1);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
        String formatedNow = oneMonthAgo.format(formatter);
        
        String finalUrl = String.format("%s?collection=kmdb_new2&listCount=20&detail=Y&sort=prodYear,1&releaseDts=%s&use=극장용&ServiceKey=%s", 
            baseUrl, formatedNow, serviceKey);

        log.info("KMDb 동기화 시작: URL = {}", finalUrl);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
        
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(finalUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
        
        String responseBody = response.getBody();
        // 받아온 데이터를 파싱 및 DB에 저장/업데이트
        if(responseBody != null) {
            saveMoviesFromKmdb(responseBody);
        }
        
        return responseBody;
    }

    @Override
    @Transactional
    public void saveMoviesFromKmdb(String jsonResponse) {

        log.debug("수신된 KMDb JSON 응답: {}", jsonResponse);

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode dataArray = rootNode.path("Data");
            if (dataArray.isEmpty() || !dataArray.isArray()) {
                log.warn("KMDb API 응답에서 Data 배열을 찾을 수 없습니다. 응답 전체: {}", jsonResponse);
                return;
            }

            JsonNode resultArray = dataArray.get(0).path("Result");
            if (resultArray.isEmpty() || !resultArray.isArray()) {
                log.warn("KMDb API 응답에서 Result 배열을 찾을 수 없습니다. 응답 전체: {}", jsonResponse);
                return;
            }

            int newCount = 0;
            int updateCount = 0;

            for (JsonNode movieNode : resultArray) {
                // DTO 활용하여 데이터 먼저 매핑
                com.yonsai.backend.dto.KmdbMovieDto dto = new com.yonsai.backend.dto.KmdbMovieDto();
                
                String docid = movieNode.path("DOCID").asText("");
                if (docid.isEmpty()) {
                    docid = movieNode.path("docid").asText("");
                }
                docid = docid.trim();
                if (docid.isEmpty()) continue;
                dto.setDocid(docid);

                String title = movieNode.path("title").asText("").trim();
                title = title.replaceAll("!HS|!HE", "").trim();
                dto.setTitle(title);

                JsonNode plotNode = movieNode.path("plots").path("plot");
                if (plotNode.isArray() && plotNode.size() > 0) {
                    dto.setPlotText(plotNode.get(0).path("plotText").asText(null));
                }

                dto.setRuntime(movieNode.path("runtime").asText("0"));
                dto.setRating(movieNode.path("rating").asText(null));

                String repRlsDate = movieNode.path("repRlsDate").asText("");
                if (!repRlsDate.isEmpty() && repRlsDate.contains("|")) {
                    repRlsDate = repRlsDate.split("\\|")[0];
                }
                dto.setReleaseDate(repRlsDate.isEmpty() ? null : repRlsDate);

                String posters = movieNode.path("posters").asText("");
                if (!posters.isEmpty()) {
                    dto.setPosters(posters.split("\\|")[0]);
                } else {
                    // 포스터 이미지가 null(빈 문자열)인 데이터는 가져오지 않도록 건너뜀
                    log.info("포스터 이미지 누락으로 영화 데이터 무시: {}", dto.getTitle());
                    continue;
                }

                // Entity에 DTO 값 반영 및 저장
                boolean isNew = false;
                boolean isPlotChanged = false;

                Movie movie = movieRepository.findById(dto.getDocid()).orElse(null);
                if (movie == null) {
                    movie = new Movie();
                    movie.setDocid(dto.getDocid());
                    isNew = true;
                    isPlotChanged = true;
                } else {
                    String existingPlot = movie.getPlot() == null ? "" : movie.getPlot();
                    String newPlot = dto.getPlotText() == null ? "" : dto.getPlotText();
                    if (!existingPlot.equals(newPlot)) {
                        isPlotChanged = true;
                    }
                }

                movie.setDocid(dto.getDocid());
                movie.setTitle(dto.getTitle());
                movie.setPlot(dto.getPlotText());
                
                try {
                    int runtimeVal = Integer.parseInt(dto.getRuntime());
                    movie.setRuntime(runtimeVal > 0 ? runtimeVal : 100);
                } catch (NumberFormatException e) {
                    movie.setRuntime(100);
                }
                
                String ratingVal = dto.getRating();
                movie.setRating((ratingVal == null || ratingVal.trim().isEmpty()) ? "12세이상관람가" : ratingVal.trim());
                movie.setReleaseDate(dto.getReleaseDate());
                movie.setPosterUrl(dto.getPosters());

                // KMDb에서 넘어온 데이터와 기존 DB 데이터가 완전히 똑같을 경우, 
                // JPA의 Dirty Checking이 동작하지 않아 updatedAt 필드가 갱신되지 않는 문제 발생.
                // 스케줄러가 '오늘 갱신된 영화'만 찾아서 상영 회차를 만들 수 있도록, 무조건 updatedAt을 현재 시간으로 강제 푸시합니다.
                // BaseEntity의 필드가 직접 접근 불가능하다면, 강제로 update_at을 갱신하는 네이티브 쿼리나 우회 방법을 사용하거나
                // 단순히 무의미한 업데이트(예: 동일한 값을 한 번 더 쓰기)로는 부족하므로
                // 간단히 title 뒤에 공백을 붙였다 떼는 방식으로 더티체킹을 유도합니다.
                if (!isNew && !isPlotChanged) {
                    movie.setTitle(movie.getTitle() + " "); // 일시적 변경
                    movieRepository.saveAndFlush(movie); // DB에 플러시하여 updatedAt 갱신 유도
                    movie.setTitle(movie.getTitle().trim()); // 원상복구
                }

                // 저장
                movie = movieRepository.save(movie);
                
                if (isNew) newCount++;
                else updateCount++;

                // Keyword 문자열 미리 추출
                String keywordsStr = movieNode.path("keywords").asText("");

                // Embedding 업데이트 (트랜잭션 분리하여 실패하더라도 메인 트랜잭션 롤백 안되게 처리)
                if (isPlotChanged && dto.getPlotText() != null && !dto.getPlotText().trim().isEmpty()) {
                    // 줄거리를 바탕으로 Gemini를 통해 핵심 장르/테마 키워드를 추가로 뽑아냅니다.
                    String aiExtractedKeywords = geminiService.extractKeywordsFromPlot(dto.getPlotText());
                    String textForVector = "장르/키워드: " + keywordsStr + ", " + aiExtractedKeywords + ". 줄거리: " + dto.getPlotText();
                    
                    // 자기 자신의 메서드 호출 시 @Transactional(REQUIRES_NEW)가 무시되는 문제를 방지하기 위해 
                    // Spring context에서 프록시를 가져오거나 try-catch로 일단 에러를 완전히 먹어버리게 구성
                    // 가장 안전한 방법은 try-catch로 Native Query Exception 을 삼켜버리는 것임.
                    try {
                        updateEmbeddingSafeProxyCall(movie.getDocid(), movie.getTitle(), textForVector);
                    } catch (Exception e) {
                        log.error("임베딩 업데이트 시도 중 에러 발생 (메인 저장 유지): {}", movie.getTitle(), e);
                    }
                }

                // Actor 처리
                JsonNode actorsNode = movieNode.path("actors").path("actor");
                if (actorsNode.isArray()) {
                    for (JsonNode actorInfo : actorsNode) {
                        String actorId = actorInfo.path("actorId").asText("");
                        String actorNm = actorInfo.path("actorNm").asText("");

                        if (!actorId.isEmpty() && !actorNm.isEmpty()) {
                            Actor actor = actorRepository.findByActorId(actorId).orElse(null);
                            if (actor == null) {
                                actor = new Actor();
                                actor.setActorId(actorId);
                                actor.setName(actorNm);
                                actor = actorRepository.save(actor);
                            }

                            if (!movieActorRepository.existsByMovieAndActor(movie, actor)) {
                                MovieActor ma = new MovieActor();
                                ma.setMovie(movie);
                                ma.setActor(actor);
                                movieActorRepository.save(ma);
                            }
                        }
                    }
                }

                // Director 처리
                JsonNode directorsNode = movieNode.path("directors").path("director");
                if (directorsNode.isArray()) {
                    for (JsonNode dirInfo : directorsNode) {
                        String dirId = dirInfo.path("directorId").asText("");
                        String dirNm = dirInfo.path("directorNm").asText("");

                        if (!dirId.isEmpty() && !dirNm.isEmpty()) {
                            Director director = directorRepository.findByDirectorId(dirId).orElse(null);
                            if (director == null) {
                                director = new Director();
                                director.setDirectorId(dirId);
                                director.setName(dirNm);
                                director = directorRepository.save(director);
                            }

                            if (!movieDirectorRepository.existsByMovieAndDirector(movie, director)) {
                                MovieDirector md = new MovieDirector();
                                md.setMovie(movie);
                                md.setDirector(director);
                                movieDirectorRepository.save(md);
                            }
                        }
                    }
                }

                // Keyword 처리
                if (!keywordsStr.isEmpty()) {
                    String[] keywordArr = keywordsStr.split(",");
                    for (String kw : keywordArr) {
                        String k = kw.trim();
                        if (k.isEmpty()) continue;

                        Keyword keyword = keywordRepository.findByKeywords(k).orElse(null);
                        if (keyword == null) {
                            keyword = new Keyword();
                            keyword.setKeywords(k);
                            keyword = keywordRepository.save(keyword);
                        }

                        if (!movieKeywordRepository.existsByMovieAndKeyword(movie, keyword)) {
                            MovieKeyword mk = new MovieKeyword();
                            mk.setMovie(movie);
                            mk.setKeyword(keyword);
                            movieKeywordRepository.save(mk);
                        }
                    }
                }
            }
            
            log.info("KMDb 영화 동기화 완료 - 신규 저장: {}건, 업데이트: {}건", newCount, updateCount);

        } catch (Exception e) {
            log.error("KMDb API 데이터 파싱 및 저장 중 오류 발생", e);
        }
    }
}
