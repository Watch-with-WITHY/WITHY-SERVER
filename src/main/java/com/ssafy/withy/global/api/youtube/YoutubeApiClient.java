package com.ssafy.withy.global.api.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.withy.global.api.youtube.dto.YoutubeVideoInfo; // 아까 만든 DTO import
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeApiClient {

    private final RestClient restClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    // [변경] 리턴 타입: Integer -> YoutubeVideoInfo (정보 꾸러미)
    public YoutubeVideoInfo fetchVideoInfo(String videoId) {
        YoutubeVideoResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.googleapis.com")
                        .path("/youtube/v3/videos")
                        .queryParam("part", "snippet") // snippet 안에 title, thumbnails, categoryId 다 있음
                        .queryParam("id", videoId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(YoutubeVideoResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            throw new CustomException(GlobalErrorCode.YOUTUBE_API_ERROR); // VIDEO_NOT_FOUND가 더 정확할 수도 있음
        }

        // 데이터 추출
        YoutubeVideoResponse.Snippet snippet = response.items().get(0).snippet();

        // 1. 카테고리 ID 변환
        Integer categoryId = Integer.parseInt(snippet.categoryId());

        // 2. 제목 추출
        String title = snippet.title();

        // 3. 최적의 썸네일 추출 (화질 우선순위: MaxRes > Standard > High > Medium > Default)
        String bestThumbnailUrl = getBestThumbnailUrl(snippet.thumbnails());

        return new YoutubeVideoInfo(title, bestThumbnailUrl, categoryId);
    }

    // 썸네일 우선순위 로직 분리
    private String getBestThumbnailUrl(YoutubeVideoResponse.Thumbnails t) {
        if (t == null) return null;
        if (t.maxres() != null) return t.maxres().url();
        if (t.standard() != null) return t.standard().url();
        if (t.high() != null) return t.high().url();
        if (t.medium() != null) return t.medium().url();
        if (t.defaultImage() != null) return t.defaultImage().url();
        return ""; // 정말 아무것도 없으면 빈 문자열 (혹은 기본 이미지 URL)
    }

    // --- JSON 매핑용 내부 DTO (유튜브 응답 구조와 일치해야 함) ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeVideoResponse(List<Item> items) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(Snippet snippet) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Snippet(
                String title,         // 제목 추가
                String categoryId,
                Thumbnails thumbnails // 썸네일 객체 추가
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Thumbnails(
                ThumbnailImage maxres,
                ThumbnailImage standard,
                ThumbnailImage high,
                ThumbnailImage medium,
                @JsonProperty("default") ThumbnailImage defaultImage // default는 자바 예약어라 매핑 필요
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ThumbnailImage(String url, Integer width, Integer height) {}
    }
}