package com.ssafy.withy.global.api.tmdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// [핵심] RestClient 테스트 전용 어노테이션 (MockServer 자동 구성)
import org.springframework.test.context.TestPropertySource;

// ... other imports

@RestClientTest(TmdbApiClient.class)
@TestPropertySource(properties = {
        "tmdb.api-key=test-key",
        "tmdb.base-url=https://api.themoviedb.org/3"
})
class TmdbApiClientTest {

    @Autowired
    private TmdbApiClient tmdbApiClient; // 테스트 대상

    @Autowired
    private MockRestServiceServer mockServer; // 가짜 서버 (TMDB 역할)

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("TMDB 검색 성공 - 넷플릭스 드라마(TV) 파싱 확인")
    void searchContent_success() {
        // given
        String query = "Stranger Things";

        // 가짜 TMDB 응답 JSON (실제 응답이랑 똑같이 만듦)
        String mockResponse = """
                    {
                        "results": [
                            {
                                "id": 66732,
                                "name": "기묘한 이야기",
                                "first_air_date": "2016-07-15",
                                "overview": "인디애나주의 작은 마을...",
                                "poster_path": "/poster.jpg",
                                "media_type": "tv"
                            }
                        ]
                    }
                """;

        // [Mocking] "이 URL로 요청이 오면, 저 JSON을 뱉어라"
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/multi"))) // URL 포함 확인
                .andRespond(withSuccess(mockResponse, APPLICATION_JSON));

        // when
        TmdbApiClient.TmdbInfo result = tmdbApiClient.searchContent(query);

        // then
        assertNotNull(result);
        assertEquals(66732, result.getTmdbId());
        assertEquals("기묘한 이야기", result.getTitle()); // name -> title 매핑 확인
        assertEquals(com.ssafy.withy.domain.content.entity.MediaType.TV, result.getMediaType()); // "tv" -> Enum.TV 확인

        System.out.println("검색 결과: " + result.getTitle() + ", " + result.getMediaType());
    }

    @Test
    @DisplayName("TMDB 검색 실패 - 결과 없음")
    void searchContent_fail_empty() {
        // given
        String mockEmptyResponse = "{ \"results\": [] }";

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/multi")))
                .andRespond(withSuccess(mockEmptyResponse, APPLICATION_JSON));

        // when & then
        assertThrows(CustomException.class, () -> tmdbApiClient.searchContent("존재하지않는영화zzzz"));
    }
}