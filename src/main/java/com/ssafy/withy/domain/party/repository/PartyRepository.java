package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Integer>, PartyRepositoryCustom {

        // 1. 검색
        // 1. 검색 (제목 or 장르명)
        @Query("SELECT DISTINCT p FROM Party p " +
                "LEFT JOIN p.content c " +
                "LEFT JOIN c.contentGenres cg " +
                "LEFT JOIN cg.genre g " +
                "WHERE (p.title LIKE %:keyword% OR g.name LIKE %:keyword%) " +
                "AND p.isDeleted = false")
        Page<Party> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

        // Legacy: 제목만 검색 (유지하거나 삭제)
        Page<Party> findByTitleContainingAndIsDeletedFalse(String title, Pageable pageable);

        // 2. 호스트로 조회
        Page<Party> findByHostAndIsDeletedFalse(User host, Pageable pageable);

        List<Party> findAllByHost(User host);

        @Query("SELECT p FROM Party p JOIN p.content c JOIN c.contentGenres cg " +
                        "WHERE cg.genre.id = :genreId AND p.isDeleted = false " +
                        "ORDER BY p.isActive DESC, p.createdAt DESC")
        List<Party> findLatestPartiesByGenre(@Param("genreId") Integer genreId, Pageable pageable);

        // 4. 인기순 조회 (JPQL 수정)
        @Query("SELECT p FROM Party p JOIN p.content c JOIN c.contentGenres cg " +
                        "WHERE cg.genre.id = :genreId AND p.isDeleted = false " +
                        "ORDER BY p.isActive DESC, p.currentParticipants DESC, p.createdAt DESC")
        List<Party> findPopularPartiesByGenre(@Param("genreId") Integer genreId, Pageable pageable);

        // 5. 이어보기 (JPQL 수정)
        @Query("SELECT p FROM Party p WHERE p.content.id = :contentId " +
                        "AND (:seasonNumber IS NULL OR p.seasonNumber = :seasonNumber) " +
                        "AND (:episodeNumber IS NULL OR p.episodeNumber = :episodeNumber) " +
                        "AND p.isDeleted = false " +
                        "AND p.actualActiveTime IS NOT NULL " +
                        "ORDER BY p.isActive DESC, p.currentParticipants DESC, p.createdAt DESC")
        List<Party> findContinueWatchingParties(
                        @Param("contentId") Integer contentId,
                        @Param("seasonNumber") Byte seasonNumber,
                        @Param("episodeNumber") Integer episodeNumber);

        // 6. 상세 조회 (Fetch Join)
        @Query("SELECT p FROM Party p JOIN FETCH p.content " +
                        "WHERE p.id = :id AND p.isDeleted = false")
        Party findWithContentById(@Param("id") Integer id);

        // 7. 락 걸고 조회
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from Party p where p.id = :id and p.isDeleted = false")
        Optional<Party> findByIdWithLock(@Param("id") Integer id);

        // 8. AI 추천용
        List<Party> findAllByIsDeletedFalse();

        List<Party> findAllByContent_TmdbIdAndIsDeletedFalse(Integer tmdbId);

        List<Party> findAllByCreatedAtAfterAndIsDeletedFalse(LocalDateTime createdAt);

        List<Party> findAllByIsDeletedFalseAndIsActiveFalseAndScheduledActiveTimeBefore(LocalDateTime limitTime);
}
