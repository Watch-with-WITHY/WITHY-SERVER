package com.ssafy.withy.domain.content.repository;

import com.ssafy.withy.domain.content.entity.ContentGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentGenreRepository extends JpaRepository<ContentGenre, Integer> {
}
