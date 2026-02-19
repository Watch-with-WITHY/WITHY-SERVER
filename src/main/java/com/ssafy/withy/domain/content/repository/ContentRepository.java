package com.ssafy.withy.domain.content.repository;

import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.party.entity.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Integer> {

    Optional<Content> findByExternalId(String externalId);

    Optional<Content> findByOriginalTitle(String originalTitle);

    Optional<Content> findByTmdbId(Integer tmdbId);
}
