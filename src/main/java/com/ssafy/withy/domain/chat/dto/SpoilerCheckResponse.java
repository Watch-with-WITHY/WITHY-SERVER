package com.ssafy.withy.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpoilerCheckResponse {

    @JsonProperty("party_id")
    private String partyId;

    @JsonProperty("results")
    private List<SpoilerResult> results;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpoilerResult {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("is_spoiler")
        private Boolean isSpoiler;

        @JsonProperty("reason")
        private String reason;
    }
}
