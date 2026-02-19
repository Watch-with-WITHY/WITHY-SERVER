package com.ssafy.withy.domain.user.repository;

import com.ssafy.withy.domain.user.entity.Subscribe;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubscribeRepository extends JpaRepository<Subscribe, Integer> {
    List<Subscribe> findByUser_IdOrderByIdAsc(Integer userId, Pageable pageable);

    List<Subscribe> findAllByUser_Id(Integer userId);

    void deleteByUser_IdAndGenre_Id(Integer userId, Integer genreId);

    boolean existsByUser_IdAndGenre_Id(Integer userId, Integer genreId);

    @Query("SELECT s.genre.id FROM Subscribe s WHERE s.user.id = :userId")
    List<Integer> findGenreIdsByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM Subscribe s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);
}
