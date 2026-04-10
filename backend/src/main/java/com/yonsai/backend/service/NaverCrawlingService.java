package com.yonsai.backend.service;

import com.yonsai.backend.dto.OttResultDto;

public interface NaverCrawlingService {
    OttResultDto getOttLinkIfAvailable(String movieTitle);
}
