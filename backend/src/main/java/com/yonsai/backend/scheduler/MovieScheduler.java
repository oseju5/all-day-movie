package com.yonsai.backend.scheduler;

import com.yonsai.backend.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonsai.backend.entity.MovieMetadata;
import com.yonsai.backend.repository.MovieMetadataRepository;
import com.yonsai.backend.service.NaverCrawlingService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * 영화 데이터 및 회차 정보를 정기적으로 업데이트하는 스케줄러.
 *
 * @author ohseju
 * @since : 2026-03-23
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MovieScheduler {

    private final MovieService movieService;
    private final MovieMetadataRepository movieMetadataRepository;

    /**
     * 매일 00:00에 실행되어 KMDB 영화 데이터를 동기화하고
     * 7일 뒤의 회차 정보를 생성합니다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void scheduleKmdbAndShowtimeSync() {
        log.info("정기 영화 데이터 동기화 및 회차 생성 스케줄러 실행 시작 (자정)");
        try {
            // 1. 먼저 KMDb 데이터를 가져와서 DB를 업데이트 합니다. (updated_at 갱신됨)
            movieService.syncKmdbAndSave();
            
            // 2. 오늘 업데이트된 영화들을 대상으로 7일 뒤 회차를 생성합니다.
            movieService.generateShowtimesForUpdatedMovies();
            
            log.info("정기 영화 데이터 동기화 및 회차 생성 스케줄러 실행 완료");
        } catch (Exception e) {
            log.error("정기 영화 데이터 동기화 및 회차 생성 중 오류 발생", e);
        }
    }

    /**
     * 매일 새벽 4시에 실행되어
     * - 30일이 지난 예약 내역
     * - 30일 동안 업데이트되지 않은 (상영 종료된) 영화 내역
     * 들을 DB에서 일괄 삭제합니다. (영화 삭제 시 회차, 티켓 등 연쇄 삭제됨)
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupOldData() {
        log.info("오래된 영화 및 예약 데이터 정리(Cleanup) 스케줄러 실행 시작 (새벽 4시)");
        try {
            movieService.deleteOldData();
            log.info("오래된 영화 및 예약 데이터 정리 스케줄러 실행 완료");
        } catch (Exception e) {
            log.error("오래된 데이터 정리 중 오류 발생", e);
        }
    }

    /**
     * 매주 월요일 새벽 3시에 실행되어 DB에 캐싱된 OTT 링크들의 유효성을 검사합니다.
     * 유효하지 않은 링크(404 등)는 invalid 처리하여 추후 크롤러가 다시 수집하도록 만듭니다. (Self-Healing)
     */
    @Scheduled(cron = "0 0 3 * * MON", zone = "Asia/Seoul")
    public void validateOttLinksBatch() {
        log.info("[Self-Healing Batch] 캐싱된 OTT 링크 유효성 검사 시작");
        List<MovieMetadata> metadataList = movieMetadataRepository.findAll();
        
        int invalidCount = 0;
        for (MovieMetadata meta : metadataList) {
            if (meta.getOttLink() != null && !meta.getOttLink().isEmpty()) {
                boolean isValid = checkUrlValidity(meta.getOttLink());
                if (!isValid) {
                    meta.setValid(false);
                    movieMetadataRepository.save(meta);
                    invalidCount++;
                    log.warn("유효하지 않은 OTT 링크 발견. 재크롤링 대상으로 마킹됨: {}", meta.getMovieTitle());
                }
            }
        }
        log.info("[Self-Healing Batch] 유효성 검사 완료. 총 {}개의 링크가 만료 처리됨.", invalidCount);
    }

    /**
     * 간단한 HttpURLConnection을 사용하여 URL의 HTTP 응답 코드가 정상(200~399)인지 확인합니다.
     */
    private boolean checkUrlValidity(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000); // 3초 타임아웃
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            return false;
        }
    }
}
