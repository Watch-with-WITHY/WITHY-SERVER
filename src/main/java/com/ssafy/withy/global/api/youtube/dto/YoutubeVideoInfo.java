package com.ssafy.withy.global.api.youtube.dto;

public record YoutubeVideoInfo(
        String title,
        String thumbnailUrl,
        Integer categoryId
) {}