package com.ssafy.withy.domain.content.service;

import com.ssafy.withy.domain.content.dto.GenreListResponseDto;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.party.entity.PlatformType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @InjectMocks
    private GenreService genreService;

    @Mock
    private GenreRepository genreRepository;

    @Test
    @DisplayName("장르 목록 조회 정상 동작 - OTT")
    void getGenreList_Success_OTT() {
        // Given
        PlatformType platform = PlatformType.OTT;
        Genre genre1 = Genre.builder().name("가족").code(1).type(platform).build();
        Genre genre2 = Genre.builder().name("공포").code(2).type(platform).build();

        given(genreRepository.findAllByTypeOrderByNameAsc(eq(platform)))
                .willReturn(List.of(genre1, genre2));

        // When
        GenreListResponseDto result = genreService.getGenreList("OTT");

        // Then
        assertThat(result.genres()).hasSize(2);
        assertThat(result.genres().get(0).name()).isEqualTo("가족");
        assertThat(result.genres().get(1).name()).isEqualTo("공포");
    }

    @Test
    @DisplayName("장르 목록 조회 실패 - 잘못된 플랫폼 타입")
    void getGenreList_Fail_InvalidPlatform() {
        // Given
        String invalidPlatform = "INVALID";

        // When & Then
        assertThatThrownBy(() -> genreService.getGenreList(invalidPlatform))
                .isInstanceOf(com.ssafy.withy.global.error.exception.CustomException.class);
    }
}
