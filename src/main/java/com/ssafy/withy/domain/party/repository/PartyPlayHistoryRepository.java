package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.party.entity.PartyPlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyPlayHistoryRepository extends JpaRepository<PartyPlayHistory, Integer> {
}
