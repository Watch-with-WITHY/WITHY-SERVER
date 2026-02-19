package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.party.entity.PartyState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyStateRepository extends JpaRepository<PartyState, Integer> {
}
