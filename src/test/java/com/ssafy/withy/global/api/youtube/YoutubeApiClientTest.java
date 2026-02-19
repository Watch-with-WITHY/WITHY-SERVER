package com.ssafy.withy.global.api.youtube;

import com.ssafy.withy.global.api.youtube.dto.YoutubeVideoInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
class YoutubeApiClientTest {

    @Autowired
    private YoutubeApiClient youtubeApiClient;

    @Test
    @DisplayName("[Real API] 유튜브 영상 ID로 정보(제목, 썸네일, 카테고리)를 가져온다")
    void fetchVideoInfo_real() { // 메서드 이름 변경
        // given
        // 싸이 - 강남스타일 (Music Category = 10)
        String videoId = "9bZkp7q19f0";

        // when
        YoutubeVideoInfo info = youtubeApiClient.fetchVideoInfo(videoId);

        // then
        System.out.println(">>> 제목: " + info.title());
        System.out.println(">>> 썸네일: " + info.thumbnailUrl());
        System.out.println(">>> 카테고리 ID: " + info.categoryId());

        assertThat(info).isNotNull();
        assertThat(info.title()).contains("Gangnam Style"); // 제목 검증
        assertThat(info.thumbnailUrl()).startsWith("https://"); // 썸네일 URL 검증
        assertThat(info.categoryId()).isEqualTo(10); // 음악(10)
    }
}