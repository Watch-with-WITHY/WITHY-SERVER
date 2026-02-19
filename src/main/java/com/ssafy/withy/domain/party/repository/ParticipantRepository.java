package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.party.entity.Participant;
import com.ssafy.withy.domain.party.entity.ParticipantStatus;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Integer> {
    Optional<Participant> findByParty_IdAndUser_Id(Integer partyId, Integer userId);

    // Alias for findByParty_IdAndUser_Id to match Service calls
    Optional<Participant> findByPartyIdAndUserId(Integer partyId, Integer userId);

    List<Participant> findAllByPartyId(Integer partyId);

    boolean existsByPartyIdAndUserIdAndStatus(Integer partyId, Integer userId,
            com.ssafy.withy.domain.party.entity.ParticipantStatus status);

    void deleteAllByUser(com.ssafy.withy.domain.user.entity.User user);

    void deleteAllByParty(Party party);

    @Query("SELECT p FROM Participant p JOIN FETCH p.user WHERE p.party.id IN :partyIds AND p.role = 'HOST'")
    List<Participant> findHostsByPartyIds(@Param("partyIds") List<Integer> partyIds);

    // 파티와 유저로 참가자 정보 조회 (ACTIVE 상태인 것만)
    Optional<Participant> findByPartyAndUserAndStatus(Party party, User user, ParticipantStatus status);
}
