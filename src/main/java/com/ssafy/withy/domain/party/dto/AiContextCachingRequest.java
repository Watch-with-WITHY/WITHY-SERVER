package com.ssafy.withy.domain.party.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiContextCachingRequest {
    @JsonProperty("movie_plot")
    private String moviePlot;

    @JsonProperty("party_id")
    private String partyId;
}
