package com.ssafy.withy.domain.party.controller;


import com.ssafy.withy.domain.party.dto.*;
import com.ssafy.withy.domain.party.entity.ParticipantRoleUpdateRequest;
import com.ssafy.withy.domain.party.entity.ParticipantStatus;
import com.ssafy.withy.domain.party.service.PartyService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.ASC;

@Validated
@Tag(name = "Party", description = "파티 관련 API")
@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class PartyController {

        private final PartyService partyService;

        private static final String CATEGORY_DESCRIPTION = "카테고리 또는 장르 코드 (선택)<br>" +
                        "<b>[OTT]</b><br>" +
                        "- KO: 국내 (한국 영화)<br>" +
                        "- FOREIGN: 해외 (외국 영화)<br>" +
                        "- 28: 액션<br>" +
                        "- 12: 모험<br>" +
                        "- 16: 애니메이션<br>" +
                        "- 35: 코미디<br>" +
                        "- 80: 범죄<br>" +
                        "- 99: 다큐멘터리<br>" +
                        "- 18: 드라마<br>" +
                        "- 10751: 가족<br>" +
                        "- 14: 판타지<br>" +
                        "- 36: 역사<br>" +
                        "- 27: 공포<br>" +
                        "- 10402: 음악<br>" +
                        "- 9648: 미스터리<br>" +
                        "- 10749: 로맨스<br>" +
                        "- 878: SF<br>" +
                        "- 10770: TV 영화<br>" +
                        "- 53: 스릴러<br>" +
                        "- 10752: 전쟁<br>" +
                        "- 37: 서부<br><br>" +
                        "<b>[YOUTUBE]</b><br>" +
                        "- 1: 영화/애니메이션<br>" +
                        "- 2: 자동차<br>" +
                        "- 10: 음악<br>" +
                        "- 15: 애완동물/동물<br>" +
                        "- 17: 스포츠<br>" +
                        "- 19: 여행/이벤트<br>" +
                        "- 20: 게임<br>" +
                        "- 22: 인물/블로그<br>" +
                        "- 23: 코미디<br>" +
                        "- 24: 엔터테인먼트<br>" +
                        "- 25: 뉴스/정치<br>" +
                        "- 26: 노하우/스타일<br>" +
                        "- 27: 교육<br>" +
                        "- 28: 과학기술<br>" +
                        "- 29: 비영리/사회운동";

        @Operation(summary = "파티 생성", description = "새로운 파티방을 생성합니다. (컨텐츠가 없으면 자동 등록됨)")
        @PostMapping
        public ResponseEntity<ApiResponse<PartyCreateResponse>> createParty(
                        @AuthenticationPrincipal CustomUserDetails userDetails, // 토큰에서 유저 정보 추출
                        @Valid @RequestBody PartyCreateRequest request) {
                // userDetails.getId() -> 토큰에 담긴 유저
                Integer partyId = partyService.createParty(request, userDetails.getUserId());

                return ResponseEntity
                                .status(GlobalSuccessCode.PARTY_CREATE_SUCCESS.getStatus())
                                .body(ApiResponse.success(GlobalSuccessCode.PARTY_CREATE_SUCCESS,
                                                new PartyCreateResponse(partyId)));
        }

        @Operation(summary = "파티 목록 조회", description = """
                        플랫폼 및 카테고리별 파티 목록을 조회합니다.

                        **응답에 포함되는 정보:**
                        - 기본 파티 정보 (제목, 플랫폼, 참가자 수 등)
                        - **currentPlaybackTime**: 현재 사용자의 콘텐츠 재생 진행 시간 (초 단위, 시청 기록 없으면 null, 인증 필요)
                        - **host**: 파티 호스트 정보 (닉네임, 프로필 이미지)
                        """, parameters = {
                        @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
                        @Parameter(name = "size", description = "한 페이지당 개수", example = "20"),
                        @Parameter(name = "sort", description = "정렬 기준 (형식: 필드명,정렬방식).<br>예시: <code>scheduledActiveTime,asc</code>(활성화 예정 시간순), <code>title,asc</code>(제목순)", example = "scheduledActiveTime,asc")
        })
        @GetMapping
        public ResponseEntity<ApiResponse<PartyListResult>> getPartyList(
                        @Parameter(description = "플랫폼 타입 (필수)<br>입력값 예시: OTT, YOUTUBE", example = "OTT") @RequestParam(name = "platform") @NotBlank(message = "플랫폼은 필수 입력값입니다.") String platform,
                        @Parameter(description = CATEGORY_DESCRIPTION, example = "KO") @RequestParam(name = "category", required = false) String category,
                        @Parameter(description = "활성화 여부 필터 (true: 활성, false: 비활성, null: 전체)", example = "true") @RequestParam(name = "isActive", required = false) Boolean isActive,
                        @Parameter(hidden = true) @PageableDefault(size = 20, sort = "scheduledActiveTime", direction = ASC) Pageable pageable,
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer userId = (userDetails != null) ? userDetails.getUserId() : null;
                Page<PartyListResponseDto> partyPage = partyService.getPartyList(platform, category, isActive, pageable, userId);
                return ResponseEntity.ok(ApiResponse.success(
                                GlobalSuccessCode.GET_PARTY_LIST_SUCCESS,
                                PartyListResult.of(partyPage)));
        }

        @Operation(summary = "통합 검색", description = """
                        키워드를 통해 파티 및 카테고리(장르)를 검색합니다.

                        **응답에 포함되는 정보:**
                        - 기본 파티 정보 (제목, 플랫폼, 참가자 수 등)
                        - **currentPlaybackTime**: 현재 사용자의 콘텐츠 재생 진행 시간 (초 단위, 시청 기록 없으면 null, 인증 필요)
                        - **host**: 파티 호스트 정보 (닉네임, 프로필 이미지)
                        """, parameters = {
                        @Parameter(name = "keyword", description = "검색어 (1글자 이상 필수)", example = "액션"),
                        @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
                        @Parameter(name = "size", description = "한 페이지당 개수", example = "20"),
                        @Parameter(name = "sort", description = "정렬 기준 (형식: 필드명,정렬방식).<br>예시: <code>scheduledActiveTime,asc</code>(활성화 예정 시간순), <code>title,asc</code>(제목순)", example = "scheduledActiveTime,asc")
        })
        @GetMapping("/search")
        public ResponseEntity<ApiResponse<IntegratedSearchResponseDto>> getIntegratedSearchList(
                        @RequestParam(name = "keyword") @NotBlank(message = "검색어는 필수 입력값입니다.") @Size(min = 1, message = "검색어는 1글자 이상이어야 합니다.") String keyword,
                        @Parameter(hidden = true) @PageableDefault(size = 20, sort = "scheduledActiveTime", direction = ASC) Pageable pageable,
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer userId = (userDetails != null) ? userDetails.getUserId() : null;
                IntegratedSearchResponseDto result = partyService.searchIntegrated(keyword, pageable, userId);
                return ResponseEntity.ok(ApiResponse.success(
                                GlobalSuccessCode.GET_PARTY_LIST_SUCCESS, result));
        }

        @Operation(summary = "내가 만든 파티 목록 조회", description = "내가 호스트로 있는 파티 목록을 조회합니다. (활성화 예정 시간순 정렬)")
        @GetMapping("/me/hosted")
        public ResponseEntity<ApiResponse<PartyListResult>> getPartiesHostedByMe(
                @Parameter(hidden = true) @PageableDefault(size = 20, sort = "scheduledActiveTime", direction = ASC) Pageable pageable,
                @AuthenticationPrincipal CustomUserDetails userDetails) {
                
                Page<PartyListResponseDto> partyPage = partyService.getPartiesHostedByMe(userDetails.getUserId(), pageable);
                
                return ResponseEntity.ok(ApiResponse.success(
                        GlobalSuccessCode.GET_PARTY_LIST_SUCCESS, 
                        PartyListResult.of(partyPage)));
        }

        @Operation(summary = "팔로우 카테고리 파티 조회", description = "내가 팔로우한 카테고리(최대 3개)의 파티 목록(최대 10개)을 조회합니다.")
        @GetMapping("/following")
        public ResponseEntity<ApiResponse<List<CategoryPartyResponseDto>>> getFollowedCategoryParties(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer userId = userDetails.getUserId();
                List<CategoryPartyResponseDto> result = partyService.getFollowedCategoryParties(userId);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_PARTY_LIST_SUCCESS, result));
        }

        @Operation(summary = "인기 카테고리 파티 조회", description = """
                        현재 인기있는 카테고리(최대 3개)의 인기 파티 목록(최대 10개)을 조회합니다.

                        **응답에 포함되는 정보:**
                        - 장르 + 파티 리스트 구조 유지
                        - 각 파티에 **currentPlaybackTime** 및 **host** 정보 포함
                        """)
        @GetMapping("/popular")
        public ResponseEntity<ApiResponse<List<CategoryPartyResponseDto>>> getPopularCategoryParties(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer userId = (userDetails != null) ? userDetails.getUserId() : null;
                List<CategoryPartyResponseDto> result = partyService.getPopularCategoryParties(userId);
                return ResponseEntity.ok(ApiResponse.success(
                                GlobalSuccessCode.GET_PARTY_LIST_SUCCESS, result));
        }

        @Operation(summary = "이어보기 추천 파티 조회", description = """
                        최근 시청 기록을 기반으로 이어보기 적합한 파티를 추천합니다.

                        **응답에 포함되는 정보:**
                        - 시청 기록 기반 파티 추천
                        - **currentPlaybackTime**: 사용자의 콘텐츠 재생 진행 시간 (초 단위, 필수 포함)
                        - **host**: 파티 호스트 정보 (닉네임, 프로필 이미지)
                        """)
        @GetMapping("/recommend/continue")
        public ResponseEntity<ApiResponse<List<PartyListResponseDto>>> getContinueWatchingRecommendations(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
                Integer userId = userDetails.getUserId();
                List<PartyListResponseDto> result = partyService.getContinueWatchingRecommendations(userId);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_PARTY_LIST_SUCCESS, result));
        }

        @Operation(summary = "플랫폼 타입 리스트 조회", description = "현재 지원하는 플랫폼 종류(OTT, YOUTUBE 등)를 반환합니다.")
        @GetMapping("/platforms/types")
        public ResponseEntity<ApiResponse<PlatformTypeResponseDto>> getPlatformTypes() {
                List<String> types = partyService.getPlatformTypes();
                return ResponseEntity.ok(ApiResponse.success(
                                GlobalSuccessCode.GET_PLATFORM_TYPES_SUCCESS,
                                PlatformTypeResponseDto.from(types)));
        }

    @Operation(summary = "파티 상세 조회", description = "파티의 상세 정보(호스트, 컨텐츠, 참여 현황 등)를 조회합니다.")
        @GetMapping("/{partyId}")
        public ResponseEntity<ApiResponse<PartyDetailResponse>> getPartyDetail(@PathVariable Integer partyId) {
                PartyDetailResponse response = partyService.getPartyDetail(partyId);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_PARTY_DETAIL_SUCCESS, response));
        }

        @Operation(summary = "파티 비밀번호 조회", description = "HOST만 조회 가능합니다.")
        @GetMapping("/{partyId}/password")
        public ResponseEntity<ApiResponse<PartyPasswordResponse>> getPartyPassword(
                @PathVariable Integer partyId,
                @AuthenticationPrincipal CustomUserDetails userDetails) {
            
            PartyPasswordResponse response = partyService.getPartyPassword(partyId, userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_PARTY_DETAIL_SUCCESS, response));
        }

        @Operation(summary = "파티 정보 수정", description = "방장만 가능합니다. 컨텐츠가 변경되면 관련 정보도 자동 갱신됩니다.")
        @PutMapping("/{partyId}")
        public ResponseEntity<ApiResponse<GlobalSuccessCode>> updateParty(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Integer partyId,
                        @RequestBody @Valid PartyUpdateRequest request) {

                partyService.updateParty(partyId, user.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.UPDATE_PARTY_SUCCESS));
        }

        @Operation(summary = "파티 활성화 (시작)", description = "파티 상태를 Active로 변경하고 시작 시간을 기록합니다.")
        @PatchMapping("/{partyId}/active")
        public ResponseEntity<ApiResponse<GlobalSuccessCode>> activateParty(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Integer partyId) {

                partyService.activateParty(partyId, user.getUserId());
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.ACTIVATE_PARTY_SUCCESS));
        }

        // [참가] 참가자가 파티에 들어감
        @Operation(summary = "파티 참가", description = "비밀번호가 설정된 방일 경우 비밀번호가 필요합니다.")
        @PostMapping("/{partyId}/participants")
        public ResponseEntity<ApiResponse<PartyEnterResponse>> enterParty(
                @PathVariable Integer partyId,
                @RequestBody(required = false) PartyEnterRequest request,
                @AuthenticationPrincipal CustomUserDetails user
        ) {
                // request가 null일 경우(비번 없는 공개방) 대비
                String password = (request != null) ? request.password() : null;

                PartyEnterResponse response = partyService.enterParty(partyId, user.getUserId(), password);

                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.PARTY_JOIN_SUCCESS, response));
        }

        // [퇴장] 참가자가 스스로 나감
        @Operation(summary = "파티 퇴장", description = "참여자가 파티에서 나갑니다.")
        @DeleteMapping("/{partyId}/participants")
        public ResponseEntity<ApiResponse<Void>> exitParty(
                @PathVariable Integer partyId,
                @AuthenticationPrincipal CustomUserDetails user
        ) {
                partyService.exitParty(partyId, user.getUserId());

                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.PARTY_EXIT_SUCCESS));
        }

        @Operation(summary = "파티 종료 (삭제)", description = "방장만 가능합니다. 파티를 비활성화(Soft Delete) 합니다.")
        @DeleteMapping("/{partyId}")
        public ResponseEntity<ApiResponse<Void>> deleteParty(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Integer partyId) {

                partyService.deleteParty(partyId, user.getUserId());
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.DELETE_PARTY_SUCCESS, null));
        }

        @Operation(summary = "참여자 목록 조회", description = "파티의 모든 참여자(Role, Status 포함)를 조회합니다.")
        @GetMapping("/{partyId}/participants")
        public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getParticipants(@PathVariable Integer partyId) {
                List<ParticipantResponse> response = partyService.getParticipants(partyId);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.GET_PARTICIPANTS_SUCCESS, response));
        }

        @Operation(summary = "참여자 강퇴/차단", description = "참여자를 내보내거나(BANNED) 채팅 금지(MUTED) 시킵니다.")
        @DeleteMapping("/{partyId}/participants/{userId}")
        public ResponseEntity<ApiResponse<Void>> banParticipant(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Integer partyId,
                        @PathVariable Integer userId,
                        @RequestParam(defaultValue = "BANNED") ParticipantStatus status) { // 기본은 강퇴

                partyService.changeParticipantState(partyId, user.getUserId(), userId, status);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.BAN_PARTICIPANT_SUCCESS, null));
        }

        @Operation(summary = "참여자 상태 변경", description = "참여자의 상태(JOINED, MUTED, BANNED)를 변경합니다. (예: 언뮤트, 언밴)")
        @PatchMapping("/{partyId}/participants/{userId}/status")
        public ResponseEntity<ApiResponse<Void>> updateParticipantStatus(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Integer partyId,
                        @PathVariable Integer userId,
                        @RequestParam ParticipantStatus status) {

                partyService.updateParticipantStatus(partyId, user.getUserId(), userId, status);
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.UPDATE_ROLE_SUCCESS, null));
        }

        @Operation(summary = "참여자 권한 변경", description = "참여자의 권한을 변경하거나 방장을 위임합니다.")
        @PatchMapping("/{partyId}/participants/{userId}")
        public ResponseEntity<ApiResponse<Void>> updateParticipantRole(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Integer partyId,
                        @PathVariable Integer userId,
                        @RequestBody @Valid ParticipantRoleUpdateRequest request) {

                partyService.updateParticipantRole(partyId, user.getUserId(), userId, request.newRole());
                return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.UPDATE_ROLE_SUCCESS, null));
        }

    @Operation(summary = "친구 초대 (DM 발송)", description = "파티 참여자가 친구에게 초대 링크를 DM으로 전송합니다.")
    @PostMapping("/{partyId}/invitations")
    public ResponseEntity<ApiResponse<Boolean>> inviteFriend(
            @PathVariable Integer partyId,
            @RequestBody PartyInvitationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        partyService.inviteFriend(partyId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(GlobalSuccessCode.CREATE_DM_MESSAGE_SUCCESS, true));
    }

    @Operation(
            summary = "AI 추천 파티 조회",
            description = "사용자의 선호 장르와 현재 활성화된 파티 정보를 기반으로 AI가 추천하는 파티 목록을 조회합니다.\n\n" +
                    "- AI 서버가 추천한 영화에 해당하는 활성 파티만 반환\n" +
                    "- AI 서버 장애 시 최근 생성된 인기 파티를 기본 추천으로 반환 (Fail-Open)\n" +
                    "- 추천 개수: 기본 4개, 최대 50개"
    )
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<PartyListResponseDto>>> getRecommendedParties(
            @Parameter(description = "추천 받을 파티 개수 (기본값: 4, 최대: 50)", example = "4")
            @RequestParam(required = false) Integer topK,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<PartyListResponseDto> recommendations = partyService.getRecommendedParties(
                userDetails.getUserId(),
                topK
        );

        return ResponseEntity.ok(ApiResponse.success(
                GlobalSuccessCode.GET_PARTY_LIST_SUCCESS,
                recommendations
        ));
    }
}
