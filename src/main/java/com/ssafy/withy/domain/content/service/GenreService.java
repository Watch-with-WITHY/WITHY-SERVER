package com.ssafy.withy.domain.content.service;

import com.ssafy.withy.domain.content.dto.GenreDto;
import com.ssafy.withy.domain.content.dto.GenreListResponseDto;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.party.entity.PlatformType;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreListResponseDto getGenreList(String platformStr) {
        PlatformType platform;

        try {
            platform = PlatformType.valueOf(platformStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(GlobalErrorCode.PLATFORM_TYPE_NOT_FOUND);
        }

        List<Genre> genres = genreRepository.findAllByTypeOrderByNameAsc(platform);
        
        List<GenreDto> genreDtos = genres.stream()
                .map(GenreDto::from)
                .toList();

        return GenreListResponseDto.from(genreDtos);
    }
}
