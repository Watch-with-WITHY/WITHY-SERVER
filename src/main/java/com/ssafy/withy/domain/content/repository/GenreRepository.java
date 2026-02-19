package com.ssafy.withy.domain.content.repository;

import com.ssafy.withy.domain.content.entity.Genre;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.ssafy.withy.domain.party.entity.PlatformType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface GenreRepository extends JpaRepository<Genre, Byte> {
    List<Genre> findByNameContaining(String name);

    List<Genre> findAllByTypeOrderByNameAsc(PlatformType type);

    @Query("SELECT g FROM Party p JOIN p.content c JOIN c.contentGenres cg JOIN cg.genre g GROUP BY g ORDER BY SUM(p.currentParticipants) DESC")
    List<Genre> findTop3PopularGenres(Pageable pageable);

    List<Genre> findByIdIn(List<Integer> ids);

    Optional<Genre> findByCode(Integer code);

    Optional<Genre> findByCodeAndType(Integer code, PlatformType type);
}
