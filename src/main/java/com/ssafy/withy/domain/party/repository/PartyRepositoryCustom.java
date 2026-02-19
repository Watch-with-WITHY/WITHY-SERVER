package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.entity.PlatformType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartyRepositoryCustom {
    Page<Party> searchParties(PlatformType platform, String category, Boolean isActive, Pageable pageable);
}
