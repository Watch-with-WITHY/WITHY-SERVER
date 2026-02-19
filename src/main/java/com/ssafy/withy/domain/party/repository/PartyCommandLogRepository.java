package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.party.entity.PartyCommandLog;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.withy.domain.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyCommandLogRepository extends JpaRepository<PartyCommandLog, Integer> {
    @Modifying
    @Query("DELETE FROM PartyCommandLog pcl WHERE pcl.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM PartyCommandLog pcl WHERE pcl.party.id = :partyId")
    void deleteAllByPartyId(@Param("partyId") Integer partyId);
}
