package com.yonsai.backend.service.impl;

import com.yonsai.backend.service.NaverCrawlingService;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import com.yonsai.backend.dto.OttResultDto;
import com.yonsai.backend.entity.MovieMetadata;
import com.yonsai.backend.repository.MovieMetadataRepository;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class NaverCrawlingServiceImpl implements NaverCrawlingService {

    private WebDriver driver;
    private final MovieMetadataRepository movieMetadataRepository;

    public NaverCrawlingServiceImpl(MovieMetadataRepository movieMetadataRepository) {
        this.movieMetadataRepository = movieMetadataRepository;
    }

    @PostConstruct
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        this.driver = new ChromeDriver(options);
    }

    @PreDestroy
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "ottLinks", key = "#p0")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "naverCrawling", fallbackMethod = "getOttLinkFallback")
    public OttResultDto getOttLinkIfAvailable(String movieTitle) {
        // 1. DB 캐시 조회 (Entity 사용)
        MovieMetadata metadata = movieMetadataRepository.findById(movieTitle).orElse(null);
        
        if (metadata != null) {
            // DB에 데이터가 있고, 최근 7일 이내에 업데이트 되었으며, 링크가 유효한지 검사
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            if (metadata.getUpdatedAt() != null && metadata.getUpdatedAt().isAfter(sevenDaysAgo) && metadata.isValid()) {
                log.info("[DB 캐시 사용] '{}' OTT 데이터 반환", movieTitle);
                
                return OttResultDto.builder()
                        .posterUrl(metadata.getPosterUrl())
                        .ottLink(metadata.getOttLink())
                        .ottName(metadata.getOttName())
                        .build();
            }
        }

        // 2. 크롤링 진행 (DB 캐시 미스 또는 만료된 경우)
        OttResultDto result = new OttResultDto();
        
        try {
            String posterQuery = URLEncoder.encode("영화 " + movieTitle + " 포토", "UTF-8");
            String posterUrl = "https://search.naver.com/search.naver?query=" + posterQuery;
            driver.get(posterUrl);
            log.info("[포스터 크롤링] URL 접속: {}", posterUrl);

            Thread.sleep(1000);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement imageBase = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div._image_base_poster")));
            
            WebElement firstImage = imageBase.findElement(By.cssSelector("ul li:first-child a img"));
            result.setPosterUrl(firstImage.getDomAttribute("src"));
            log.info("[포스터 크롤링 성공] '{}': {}", movieTitle, result.getPosterUrl());
        } catch (Exception e) {
            log.warn("'{}'의 포스터 이미지를 찾지 못했습니다.", movieTitle);
        }

        try {
            String ottQuery = URLEncoder.encode("영화 " + movieTitle + " 보러가기", "UTF-8");
            String ottUrl = "https://search.naver.com/search.naver?query=" + ottQuery;
            driver.get(ottUrl);
            log.info("[OTT 크롤링] URL 접속: {}", ottUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement ottLinkElement = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("._au_movie_content_wrap .price_area a.btn_box")));
            String actualLink = ottLinkElement.getDomAttribute("href");
            if (actualLink == null || actualLink.isEmpty()) {
                actualLink = ottLinkElement.getDomAttribute("cru");
            }
            
            if (actualLink != null && !actualLink.isEmpty()) {
                result.setOttLink(actualLink);
                try {
                    WebElement contentWrap = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("._au_movie_content_wrap")));
                    WebElement ottNameElement = contentWrap.findElement(By.cssSelector(".thumb a"));
                    result.setOttName(ottNameElement.getDomAttribute("class"));
                } catch (Exception ignored) {
                    result.setOttName("OTT 바로가기");
                }
                log.info("[OTT 크롤링 성공] '{}': {}", movieTitle, result.getOttLink());
            }
        } catch (Exception e) {
            log.info("'{}'의 바로보기 버튼(OTT)을 찾지 못했습니다.", movieTitle);
            throw new RuntimeException("Naver Crawling 실패", e);
        }
        
        if (result.getOttLink() == null && result.getPosterUrl() == null) {
            log.info("[검색 결과 없음] '{}'에 대한 포스터 및 OTT 데이터를 찾지 못했습니다.", movieTitle);
            return null; // 호출한 쪽에 데이터가 없음을 알림
        }
        
        
        // 3. DB에 크롤링 결과 캐싱 (DTO에서 얻은 데이터를 Entity에 세팅 후 DB에 저장)
        if (metadata == null) {
            metadata = new MovieMetadata();
            metadata.setMovieTitle(movieTitle);
        }
        metadata.setPosterUrl(result.getPosterUrl());
        metadata.setOttLink(result.getOttLink());
        metadata.setOttName(result.getOttName());
        metadata.setValid(true);
        movieMetadataRepository.save(metadata); // 영속성 컨텍스트를 통한 저장
        
        log.info("[DB 캐시 갱신] '{}' OTT 데이터 저장 완료", movieTitle);

        return result; // 컨트롤러로는 DTO 반환
    }
    
    public OttResultDto getOttLinkFallback(String movieTitle, Throwable t) {
        log.warn("Circuit Breaker 동작: 네이버 크롤링 서버 과부하 또는 오류. ('{}') - {}", movieTitle, t.getMessage());
        return null;
    }
}