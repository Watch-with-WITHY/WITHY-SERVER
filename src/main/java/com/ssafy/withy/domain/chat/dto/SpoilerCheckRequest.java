package com.ssafy.withy.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SpoilerCheckRequest {

    @JsonProperty("movie_plot")
    private String moviePlot;

    @JsonProperty("party_id")
    private String partyId;

    @JsonProperty("messages")
    private List<ChatLogDto> messages;
}
