package com.ssafy.withy.domain.party.service;

import com.ssafy.withy.domain.content.entity.*;
import com.ssafy.withy.domain.content.client.AiRefinementClient;
import com.ssafy.withy.domain.content.dto.GenreDto;
import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.entity.WatchHistory;
import com.ssafy.withy.domain.content.repository.ContentRagContextRepository;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.content.repository.WatchHistoryRepository;
import com.ssafy.withy.domain.content.service.ContentService;
import com.ssafy.withy.domain.dm.dto.DmMessageRequest;
import com.ssafy.withy.domain.party.dto.*;
import com.ssafy.withy.domain.party.entity.*;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.entity.Subscribe;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.SubscribeRepository;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.domain.party.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.Objects;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import com.ssafy.withy.domain.party.repository.PartyCommandLogRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyCommandLogRepository partyCommandLogRepository;
    private final GenreRepository genreRepository;
    private final SubscribeRepository subscribeRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final ContentService contentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PartySessionManager partySessionManager;
    private final com.ssafy.withy.domain.dm.service.DmService dmService;
    private final com.ssafy.withy.domain.party.client.AiRecommendationClient aiRecommendationClient;
    private final com.ssafy.withy.domain.party.client.AiContextCachingClient aiContextCachingClient;
    private final ContentRagContextRepository contentRagContextRepository;
    private final AiRefinementClient aiRefinementClient;

    @org.springframework.beans.factory.annotation.Value("${spring.frontend.base-url}")
    private String frontendBaseUrl;

    private static final int SUBSCRIBED_GENRE_LIMIT = 3;
    private static final int PARTY_LIMIT_PER_GENRE = 10;

    @Transactional
    public Integer createParty(PartyCreateRequest request, Integer userId) {
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (request.scheduledActiveTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new CustomException(GlobalErrorCode.INVALID_PARTY_TIME);
        }

        // 1. 컨텐츠 조회 or 생성
        Content content = contentService.getOrCreateContent(
                request.contentId(),
                request.contentTitle(),
                request.platform());
        // 2. 파티 생성
        Party party = Party.builder()
                .content(content)
                .host(host)
                .platform(request.platform())
                .title(request.title())
                .scheduledActiveTime(request.scheduledActiveTime())
                .isActive(false)
                .isPrivate(request.isPrivate() != null && request.isPrivate())
                .password(request.password())
                .maxParticipants(request.maxParticipants())
                .currentParticipants(1)
                .build();

        Party savedParty = partyRepository.save(party);

        // 3. 방장(Host)을 참여자로 등록
        Participant hostParticipant = Participant.builder()
                .party(savedParty)
                .user(host)
                .role(ParticipantRole.HOST)
                .status(ParticipantStatus.JOINED)
                .build();
        participantRepository.save(hostParticipant);

        // 4. AI 컨텍스트 캐싱 및 정제 요청
        Optional<ContentRagContext> ragContext = contentRagContextRepository.findById(content.getId());

        String contextText;
        if (ragContext.isPresent()) {
            // Case A: RAG 데이터가 존재하면 -> RAG Text로 캐싱
            contextText = ragContext.get().getRagText();
            log.info("Using RAG context for caching. PartyID: {}", savedParty.getId());
        } else {
            // Case B: RAG 데이터가 없으면 -> 기본 줄거리(Overview)로 캐싱 (Fallback)
            contextText = content.getOverview();
            log.info("RAG context missing. Using Overview for caching. PartyID: {}", savedParty.getId());
            
            // 정제 요청도 함께 진행
            aiRefinementClient.requestRefinement(content);
        }
        
        // 캐싱 요청 (비동기)
        if (contextText != null && !contextText.isBlank()) {
            aiContextCachingClient.cacheContext(String.valueOf(savedParty.getId()), contextText);
        }

        return savedParty.getId();
    }
    
    /**
     * 파티 정보 수정 (방장만 가능)
     * - 컨텐츠가 변경되었으면 ContentService를 통해 새로 가져오거나 갱신
     */
    @Transactional
    public void updateParty(Integer partyId, Integer userId, PartyUpdateRequest request) {
        Party party = getPartyOrThrow(partyId);
        validateHost(party, userId); // 방장 권한 확인

        // 플랫폼 변경 시도 차단
        if (party.getPlatform() != request.platform()) {
            throw new CustomException(GlobalErrorCode.PARTY_CANNOT_CHANGE_PLATFORM);
        }

        // 컨텐츠 변경 여부 확인
        Content targetContent = party.getContent();
        boolean isContentChanged = false;

        if (request.contentId() != null && !request.contentId().equals(party.getContent().getExternalId())) {
            // 변경됐다면 getOrCreateContent 호출
            targetContent = contentService.getOrCreateContent(
                    request.contentId(),
                    request.contentTitle(),
                    request.platform());
            isContentChanged = true;
        }

        // 엔티티 업데이트
        party.update(
                request.title(),
                request.maxParticipants(),
                request.isPrivate(),
                request.password(),
                targetContent,
                request.platform());

        // 컨텐츠 변경 시 AI 컨텍스트 캐싱 및 정제 재요청
        if (isContentChanged) {
            Optional<ContentRagContext> ragContext = contentRagContextRepository.findById(targetContent.getId());
            
            String contextText;
            if (ragContext.isPresent()) {
                contextText = ragContext.get().getRagText();
                log.info("Using RAG context for caching (Update). PartyID: {}", partyId);
            } else {
                contextText = targetContent.getOverview();
                log.info("RAG context missing. Using Overview for caching (Update). PartyID: {}", partyId);
                aiRefinementClient.requestRefinement(targetContent);
            }
            
            if (contextText != null && !contextText.isBlank()) {
                aiContextCachingClient.cacheContext(String.valueOf(partyId), contextText);
            }
        }
    }
    public Page<PartyListResponseDto> getPartyList(String platformStr, String category, Boolean isActive, Pageable pageable,
            Integer userId) {
        PlatformType platform;
        try {
            platform = PlatformType.valueOf(platformStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(GlobalErrorCode.PLATFORM_TYPE_NOT_FOUND);
        }

        Page<Party> parties = partyRepository.searchParties(platform, category, isActive, pageable);

        // 일괄 변환으로 N+1 방지
        List<PartyListResponseDto> dtos = convertToPartyListResponseDtos(parties.getContent(), userId);
        return new org.springframework.data.domain.PageImpl<>(
                dtos, pageable, parties.getTotalElements());
    }

    public Page<PartyListResponseDto> getPartiesHostedByMe(Integer userId, Pageable pageable) {
        User user = getUserOrThrow(userId);
        Page<Party> parties = partyRepository.findByHostAndIsDeletedFalse(user, pageable);

        // 일괄 변환
        List<PartyListResponseDto> dtos = convertToPartyListResponseDtos(parties.getContent(), userId);

        return new org.springframework.data.domain.PageImpl<>(
                dtos, pageable, parties.getTotalElements());
    }

    public IntegratedSearchResponseDto searchIntegrated(String keyword, Pageable pageable, Integer userId) {
        List<Genre> genres = genreRepository.findByNameContaining(keyword);
        Page<Party> parties = partyRepository.findByKeyword(keyword, pageable);

        List<GenreDto> genreDtos = genres.stream()
                .map(GenreDto::from)
                .toList();

        // 일괄 변환으로 N+1 방지
        List<PartyListResponseDto> partyDtos = convertToPartyListResponseDtos(parties.getContent(), userId);

        return IntegratedSearchResponseDto.of(genreDtos, partyDtos);
    }

    public List<CategoryPartyResponseDto> getFollowedCategoryParties(Integer userId) {
        // 모든 팔로우한 장르 조회 (파티 개수로 정렬하기 위해)
        List<Subscribe> subscribes = subscribeRepository.findByUser_IdOrderByIdAsc(userId,
                PageRequest.of(0, 50));

        // 각 장르별 파티 조회 후 파티 개수 기준으로 정렬
        return subscribes.stream()
                .map(subscribe -> {
                    Genre genre = subscribe.getGenre();
                    List<Party> parties = partyRepository
                            .findLatestPartiesByGenre(genre.getId(), PageRequest.of(0, 10));

                    // 일괄 변환으로 N+1 방지
                    List<PartyListResponseDto> partyDtos = convertToPartyListResponseDtos(parties, userId);

                    return CategoryPartyResponseDto.of(GenreDto.from(genre), partyDtos);
                })
                .sorted(Comparator.comparingInt((CategoryPartyResponseDto dto) -> dto.parties().size()).reversed())
                .limit(SUBSCRIBED_GENRE_LIMIT) // 파티가 많은 상위 3개 카테고리만
                .toList();
    }

    public List<CategoryPartyResponseDto> getPopularCategoryParties(Integer userId) {
        List<Genre> genres = genreRepository.findTop3PopularGenres(PageRequest.of(0, SUBSCRIBED_GENRE_LIMIT));

        return genres.stream()
                .map(genre -> {
                    List<Party> parties = partyRepository
                            .findPopularPartiesByGenre(genre.getId(), PageRequest.of(0, PARTY_LIMIT_PER_GENRE));

                    // 일괄 변환으로 N+1 방지
                    List<PartyListResponseDto> partyDtos = convertToPartyListResponseDtos(parties, userId);

                    return CategoryPartyResponseDto.of(GenreDto.from(genre), partyDtos);
                })
                .toList();
    }

    private PartyListResponseDto convertToPartyListResponseDto(Party party, Integer userId) {
        List<String> genreNames = List.of();
        if (party.getContent() != null && party.getContent().getContentGenres() != null) {
            genreNames = party.getContent().getContentGenres().stream()
                    .map(ContentGenre::getGenre)
                    .map(Genre::getName)
                    .toList();
        }

        // 호스트 정보 조회
        PartyListResponseDto.HostInfo host = getHostInfo(party.getId());

        // 시청 기록 조회 (인증된 사용자만)
        Integer currentPlaybackTime = null;
        if (userId != null && party.getContent() != null) {
            currentPlaybackTime = getPlaybackTime(userId, party.getContent().getId());
        }

        return PartyListResponseDto.from(party, genreNames, currentPlaybackTime, host);
    }

    private List<PartyListResponseDto> convertToPartyListResponseDtos(List<Party> parties, Integer userId) {
        if (parties.isEmpty()) {
            return List.of();
        }

        // 파티 ID 리스트 추출
        List<Integer> partyIds = parties.stream()
                .map(Party::getId)
                .toList();

        // 호스트 정보 일괄 조회
        Map<Integer, PartyListResponseDto.HostInfo> hostMap = getHostInfoMap(partyIds);

        // 시청 기록 일괄 조회 (인증된 사용자만)
        Map<Integer, Integer> playbackTimeMap = new java.util.HashMap<>();
        if (userId != null) {
            List<Integer> contentIds = parties.stream()
                    .filter(p -> p.getContent() != null)
                    .map(p -> p.getContent().getId())
                    .distinct()
                    .toList();
            playbackTimeMap = getPlaybackTimeMap(userId, contentIds);
        }

        // DTO 변환
        final Map<Integer, Integer> finalPlaybackTimeMap = playbackTimeMap;
        return parties.stream()
                .map(party -> {
                    List<String> genreNames = List.of();
                    if (party.getContent() != null && party.getContent().getContentGenres() != null) {
                        genreNames = party.getContent().getContentGenres().stream()
                                .map(ContentGenre::getGenre)
                                .map(Genre::getName)
                                .toList();
                    }

                    PartyListResponseDto.HostInfo host = hostMap.getOrDefault(party.getId(), null);
                    Integer playbackTime = party.getContent() != null
                            ? finalPlaybackTimeMap.getOrDefault(party.getContent().getId(), null)
                            : null;

                    return PartyListResponseDto.from(party, genreNames, playbackTime, host);
                })
                .toList();
    }

    /**
     * 단일 파티의 호스트 정보 조회
     */
    private PartyListResponseDto.HostInfo getHostInfo(Integer partyId) {
        List<Participant> hosts = participantRepository.findHostsByPartyIds(List.of(partyId));
        if (hosts.isEmpty()) {
            return null;
        }
        User hostUser = hosts.get(0).getUser();
        return new PartyListResponseDto.HostInfo(
                hostUser.getId(),
                hostUser.getNickname(),
                hostUser.getProfileImageUrl());
    }

    /**
     * 여러 파티의 호스트 정보를 일괄 조회하여 Map으로 반환
     */
    private Map<Integer, PartyListResponseDto.HostInfo> getHostInfoMap(List<Integer> partyIds) {
        List<Participant> hosts = participantRepository.findHostsByPartyIds(partyIds);
        return hosts.stream()
                .collect(Collectors.toMap(
                        participant -> participant.getParty().getId(),
                        participant -> {
                            User hostUser = participant.getUser();
                            return new PartyListResponseDto.HostInfo(
                                    hostUser.getId(),
                                    hostUser.getNickname(),
                                    hostUser.getProfileImageUrl());
                        }));
    }

    /**
     * 단일 콘텐츠의 재생 시간 조회
     */
    private Integer getPlaybackTime(Integer userId, Integer contentId) {
        List<WatchHistory> histories = watchHistoryRepository
                .findLatestByUserIdAndContentIds(userId, List.of(contentId));
        if (histories.isEmpty()) {
            return null;
        }
        return histories.get(0).getLastPosition();
    }

    /**
     * 여러 콘텐츠의 재생 시간을 일괄 조회하여 Map으로 반환
     */
    private Map<Integer, Integer> getPlaybackTimeMap(Integer userId, List<Integer> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        List<WatchHistory> histories = watchHistoryRepository
                .findLatestByUserIdAndContentIds(userId, contentIds);
        return histories.stream()
                .collect(Collectors.toMap(
                        wh -> wh.getContent().getId(),
                        WatchHistory::getLastPosition));
    }

    public List<PartyListResponseDto> getContinueWatchingRecommendations(Integer userId) {
        WatchHistory recentHistory = watchHistoryRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);

        if (recentHistory == null || recentHistory.getContent() == null) {
            return List.of();
        }

        List<Party> parties = partyRepository.findContinueWatchingParties(
                recentHistory.getContent().getId(),
                recentHistory.getSeasonNumber(),
                recentHistory.getEpisodeNumber());

        List<Party> sortedParties = parties.stream()
                .sorted(Comparator.comparingLong(party -> {
                    long partyPlayTime = ChronoUnit.SECONDS.between(party.getActualActiveTime(), LocalDateTime.now());
                    return Math.abs(partyPlayTime - recentHistory.getLastPosition());
                }))
                .limit(SUBSCRIBED_GENRE_LIMIT)
                .toList();

        // 일괄 변환으로 N+1 방지
        return convertToPartyListResponseDtos(sortedParties, userId);
    }

    public List<String> getPlatformTypes() {
        return List.of(PlatformType.values()).stream()
                .map(Enum::name)
                .toList();
    }

    /**
     * 파티 상세 조회
     */
    public PartyDetailResponse getPartyDetail(Integer partyId) {
        Party party = getPartyOrThrow(partyId);
        return PartyDetailResponse.from(party);
    }



    /**
     * 파티 활성화 (방장만 가능)
     */
    @Transactional
    public void activateParty(Integer partyId, Integer userId) {
        Party party = getPartyOrThrow(partyId);
        validateHost(party, userId);

        if (party.getIsActive()) {
            throw new CustomException(GlobalErrorCode.PARTY_ALREADY_ACTIVE);
        }

        party.activate();
    }

    public PartyPasswordResponse getPartyPassword(Integer partyId, Integer userId) {
        Party party = getPartyOrThrow(partyId);
        
        if (!party.getHost().getId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.PARTY_HOST_AUTH_REQUIRED);
        }

        return new PartyPasswordResponse(party.getPassword());
    }

    // ================= [입장 Logic] =================

    @Transactional
    public PartyEnterResponse enterParty(Integer partyId, Integer userId, String password) {

        // 1. 비관적 락으로 파티 조회
        Party party = partyRepository.findByIdWithLock(partyId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTY_NOT_FOUND));

        User user = getUserOrThrow(userId);

        // 2. 검증 로직
        if (participantRepository.existsByPartyIdAndUserIdAndStatus(
                partyId, userId, ParticipantStatus.BANNED)) {
            throw new CustomException(GlobalErrorCode.PARTY_ALREADY_BANNED_USER);
        }

        // 호스트 재입장 여부 확인
        boolean isHostReentry = false;
        Participant existingParticipant = null;

        if (participantRepository.existsByPartyIdAndUserIdAndStatus(
                partyId, userId, ParticipantStatus.JOINED)) {
            // 호스트가 아닌 경우 이미 참여 중 에러
            if (party.getHost() == null || !party.getHost().equals(user)) {
                throw new CustomException(GlobalErrorCode.PARTY_ALREADY_JOINED_USER);
            }
            // 호스트 재입장: 기존 Participant 재사용
            isHostReentry = true;
            existingParticipant = participantRepository.findByParty_IdAndUser_Id(partyId, userId)
                    .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTICIPANT_NOT_FOUND));
        }

        // 3. 인원 초과 확인 (호스트 재입장은 제외)
        if (!isHostReentry && party.getCurrentParticipants() >= party.getMaxParticipants()) {
            throw new CustomException(GlobalErrorCode.PARTY_FULL);
        }

        // 4. 비밀번호 검증
        if (party.getIsPrivate()) {
            if (password == null || !party.getPassword().equals(password)) {
                throw new CustomException(GlobalErrorCode.PARTY_INVALID_PASSWORD);
            }
        }

        // 5. 입장 처리
        Participant participant;
        if (isHostReentry) {
            // 호스트 재입장: 기존 Participant 재사용 (중복 생성 방지)
            participant = existingParticipant;
            // 호스트가 돌아왔으므로 삭제 스케줄 취소
            if (participant.getRole() == ParticipantRole.HOST) {
                partySessionManager.cancelHostCleanup(partyId);
            }
        } else {
            // 신규 입장: 새로운 Participant 생성
            participant = Participant.builder()
                    .party(party)
                    .user(user)
                    .role(ParticipantRole.GUEST)
                    .status(ParticipantStatus.JOINED)
                    .build();

            participantRepository.save(participant);
            party.increaseCurrentParticipants();
        }

        // 재생 정보 조회
        Integer currentPlaybackTime = 0;
        MediaType mediaType = null;
        Integer contentId = null;
        Integer tmdbId = null;
        String externalId = null;
        String title = null;
        String thumbnail = null;

        if (party.getContent() != null) {
            mediaType = party.getContent().getMediaType();
            contentId = party.getContent().getId();
            tmdbId = party.getContent().getTmdbId();
            externalId = party.getContent().getExternalId();
            title = party.getContent().getTitle();
            thumbnail = party.getContent().getPosterPath();

            if (party.getIsActive()) {
                currentPlaybackTime = getPlaybackTime(userId, contentId);
                // null이면 0으로 처리 (처음 시청)
                if (currentPlaybackTime == null) {
                    currentPlaybackTime = 0;
                }
            }
        }

        return PartyEnterResponse.of(
                partyId,
                participant.getRole(),
                party.getIsActive(),
                party.getPlatform(),
                mediaType,
                contentId,
                tmdbId,
                externalId,
                title,
                thumbnail,
                party.getSeasonNumber(),
                party.getEpisodeNumber(),
                currentPlaybackTime);
    }

    @Transactional
    public void exitParty(Integer partyId, Integer userId) {
        // 1. 파티 조회 (비관적 락 or 일반 조회)
        // 퇴장 시에도 동시성 문제가 생길 수 있으니 락을 거는 게 안전함
        Party party = partyRepository.findByIdWithLock(partyId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTY_NOT_FOUND));

        User user = getUserOrThrow(userId);

        // 2. 참가자 정보 조회
        Participant participant = participantRepository.findByParty_IdAndUser_Id(partyId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTICIPANT_NOT_FOUND));

        // 3. 방장인지 확인
        boolean isHost = party.getHost().equals(user);

        if (isHost) {
            if (party.getIsActive()) {
                this.deleteParty(partyId, userId);
            } else {
                // 방장인 경우 아무것도 하지 않고 return
                return;
            }

        } else {
            // 1. 참가자 명단에서 삭제
            participantRepository.delete(participant);

            // 2. 파티 인원 감소
            party.decreaseCurrentParticipants();
        }
    }

    @Transactional
    public void deleteParty(Integer partyId, Integer userId) {
        Party party = partyRepository.findByIdWithLock(partyId) // 안전하게 락 걸고 조회 추천
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTY_NOT_FOUND));

        User user = getUserOrThrow(userId);

        if (!party.getHost().equals(user)) {
            throw new CustomException(GlobalErrorCode.PARTY_NOT_PARTY_HOST);
        }

        // 1. 파티 종료 알림 브로드캐스팅 (참가자 삭제 전에 먼저 전송)
        var message = java.util.Map.of(
                "type", "PARTY_DELETED",
                "reason", "HOST_DELETED",
                "partyId", partyId);
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);

        // 2. 참가자 전원 삭제 (방장 포함) -> 그래야 이 유저들이 다른 파티에 갈 수 있음!
        participantRepository.deleteAllByParty(party);

        // 3. 파티 커맨드 로그 삭제
        partyCommandLogRepository.deleteAllByPartyId(partyId);

        // 4. 파티 Soft Delete 처리
        party.deleteParty(); // Party 엔티티의 메서드 (isDeleted=true, isActive=false)
    }

    /**
     * 시스템에 의한 파티 종료 (호스트 미복귀 시 자동 삭제)
     */
    @Transactional
    public void deletePartySystem(Integer partyId) {
        // 이미 없을 수도 있으므로 확인
        Party party = partyRepository.findById(partyId).orElse(null);
        if (party == null || !party.getIsActive()) {
            return;
        }

        party.deactivate();

        // 종료 알림 브로드캐스팅
        var message = java.util.Map.of(
                "type", "PARTY_ENDED",
                "reason", "HOST_DISCONNECTED");
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);
    }

    /**
     * 참여자 목록 조회
     */
    public List<ParticipantResponse> getParticipants(Integer partyId) {
        // 파티 존재 확인
        if (!partyRepository.existsById(partyId)) {
            throw new CustomException(GlobalErrorCode.PARTY_NOT_FOUND);
        }

        return participantRepository.findAllByPartyId(partyId).stream()
                .map(participant -> ParticipantResponse.from(
                        participant,
                        partySessionManager.isUserOnline(partyId, participant.getUser().getId())))
                .toList();
    }

    /**
     * WebSocket 연결 해제 시 처리 (Grace Period 적용)
     */
    @Transactional
    public void handleDisconnect(Integer partyId, Integer userId) {
        Participant participant = participantRepository.findByPartyIdAndUserId(partyId, userId)
                .orElse(null);

        if (participant == null) {
            return;
        }

        if (participant.getRole() == ParticipantRole.HOST) {
            // 호스트인 경우: 즉시 삭제하지 않고 유예 기간(Grace Period) 부여
            partySessionManager.scheduleHostCleanup(partyId);
        } else {
            // 게스트인 경우: 즉시 퇴장 처리
            leaveParty(partyId, userId);
        }
    }

    /**
     * 참여자 강퇴/차단 (DELETE)
     * - status: BANNED (default) or MUTED
     */
    @Transactional
    public void changeParticipantState(Integer partyId, Integer requesterId, Integer targetUserId,
            ParticipantStatus status) {
        Party party = getPartyOrThrow(partyId);
        Participant requester = getParticipantOrThrow(partyId, requesterId);
        Participant target = getParticipantOrThrow(partyId, targetUserId);

        // 권한 검증: 방장은 누구든 가능, 매니저는 일반 게스트만 가능
        validateAuthority(requester, target);

        // 상태 변경 (BANNED or MUTED)
        target.changeStatus(status);

        // 파티 인원 감소 (강퇴 시에만)
        if (status == ParticipantStatus.BANNED) {
            party.decreaseCurrentParticipants();
        }

        // 알림 브로드캐스팅
        // type: USER_BANNED or USER_MUTED
        var message = java.util.Map.of(
                "type", "USER_" + status.name(),
                "userId", targetUserId);
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);
    }

    /**
     * 참여자 상태 변경 (JOINED 포함 - UNBAN/UNMUTE 용)
     */
    @Transactional
    public void updateParticipantStatus(Integer partyId, Integer requesterId, Integer targetUserId,
            ParticipantStatus status) {
        // Party party = getPartyOrThrow(partyId); // Not strictly needed if
        // getParticipantOrThrow checks party relation but safer
        Participant requester = getParticipantOrThrow(partyId, requesterId);
        Participant target = getParticipantOrThrow(partyId, targetUserId);

        validateAuthority(requester, target);

        target.changeStatus(status);

        // Broadcast
        var message = java.util.Map.of(
                "type", "PARTICIPANT_STATUS_UPDATE",
                "userId", targetUserId,
                "status", status.name());
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);
    }

    @Transactional
    public void inviteFriend(Integer partyId, Integer senderId, PartyInvitationRequest request) {
        // 1. 파티 정보 및 요청자 권한 확인
        Party party = getPartyOrThrow(partyId);
        getParticipantOrThrow(partyId, senderId); // 참여자만 초대 가능

        // 2. DM 방 확보 (없으면 생성)
        var roomResponse = dmService.createRoom(senderId, request.targetUserId());

        // 3. 초대 메시지 구성
        String inviteMessage = String.format(
                "💌 [초대장] %s\n\n%s\n\n👇 파티 구경가기\n%s/waiting-room/%d",
                party.getTitle(),
                request.message() != null ? request.message() : "같이 봐요!",
                frontendBaseUrl,
                partyId);

        // 4. 메시지 전송
        dmService.sendMessage(senderId, new DmMessageRequest(roomResponse.getRoomId(), inviteMessage));
    }

    /**
     * 참여자 권한 변경 (방장 위임 포함)
     */
    @Transactional
    public void updateParticipantRole(Integer partyId, Integer requesterId, Integer targetUserId,
            ParticipantRole newRole) {
        Party party = getPartyOrThrow(partyId);
        Participant requester = getParticipantOrThrow(partyId, requesterId);
        Participant target = getParticipantOrThrow(partyId, targetUserId);

        // 방장 위임 (Host Transfer)
        if (newRole == ParticipantRole.HOST) {
            validateHost(party, requesterId); // 현재 방장만 위임 가능

            // 기존 방장 -> MANAGER로 강등
            requester.changeRole(ParticipantRole.MANAGER);
            // 새 방장 -> HOST로 승급
            target.changeRole(ParticipantRole.HOST);
            // 파티 엔티티의 host 정보 교체
            party.changeHost(target.getUser());

            // 알림 브로드캐스팅
            var message = java.util.Map.of(
                    "type", "HOST_CHANGED",
                    "oldHostId", requesterId,
                    "newHostId", targetUserId);
            messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);

            return;
        }

        // 일반 권한 변경 (매니저 임명/해임)
        validateHost(party, requesterId); // 권한 변경은 방장만 가능
        target.changeRole(newRole);

        // 알림 브로드캐스팅
        var message = java.util.Map.of(
                "type", "ROLE_UPDATE",
                "userId", targetUserId,
                "role", newRole.name());
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);
    }

    /**
     * 파티 나가기 (WebSocket 끊김 시 호출)
     * - 게스트인 경우: 참여자 정보 삭제 및 카운트 감소
     * - 호스트인 경우: 삭제하지 않음 (방 유지)
     */
    @Transactional
    public void leaveParty(Integer partyId, Integer userId) {
        Party party = getPartyOrThrow(partyId);
        Participant participant = participantRepository.findByPartyIdAndUserId(partyId, userId)
                .orElse(null);

        // 이미 나갔거나 없는 유저면 무시
        if (participant == null) {
            return;
        }

        // 호스트는 나가지지 않음 (방 폭파 전까지 유지)
        if (party.getHost().getId().equals(userId)) {
            return;
        }

        // 1. 참여자 삭제
        participantRepository.delete(participant);

        // 2. 참여자 수 감소
        party.decreaseCurrentParticipants();

        // 3. 퇴장 알림 브로드캐스팅
        var message = java.util.Map.of(
                "type", "USER_LEFT",
                "userId", userId,
                "nickname", participant.getUser().getNickname());
        messagingTemplate.convertAndSend("/sub/party/" + partyId + "/extension", message);
    }

    @Transactional
    public int cleanupExpiredParties() {
        // 1. 기준 시간 설정 (예: 예정 시간보다 30분 지났으면 삭제)
        LocalDateTime limitTime = LocalDateTime.now().minusMinutes(30);

        // 2. 삭제 대상 조회
        List<Party> expiredParties = partyRepository
                .findAllByIsDeletedFalseAndIsActiveFalseAndScheduledActiveTimeBefore(limitTime);

        int count = 0;
        for (Party party : expiredParties) {
            // 3-1. 참가자 전원 해산 (자유의 몸)
            participantRepository.deleteAllByParty(party);

            // 3-2. 파티 Soft Delete
            party.deleteParty(); // isDeleted=true, isActive=false
            count++;
        }

        return count; // 몇 개 지웠는지 리턴 (로그용)
    }

    // --- Helper Methods ---
    private Participant getParticipantOrThrow(Integer partyId, Integer userId) {
        return participantRepository.findByPartyIdAndUserId(partyId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTICIPANT_NOT_FOUND)); // 에러코드 확인 필요
    }

    // 권한 검증 메서드
    private void validateAuthority(Participant requester, Participant target) {
        // 방장은 무적
        if (requester.getRole() == ParticipantRole.HOST)
            return;

        // 매니저는 게스트만 처벌 가능
        if (requester.getRole() == ParticipantRole.MANAGER && target.getRole() == ParticipantRole.GUEST)
            return;

        throw new CustomException(GlobalErrorCode.NOT_ENOUGH_AUTHORITY); // 권한 부족
    }

    private Party getPartyOrThrow(Integer partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.PARTY_NOT_FOUND));
    }

    private User getUserOrThrow(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }

    private void validateHost(Party party, Integer userId) {
        if (!party.getHost().getId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.PARTY_NOT_PARTY_HOST);
        }
    }

    /**
     * AI 기반 추천 파티 조회
     * - 사용자 선호 장르와 활성 파티 정보를 AI 서버에 전송
     * - AI 추천 결과를 파티 목록으로 변환
     * - AI 서버 장애 시 Fallback: 최근 생성된 인기 파티 반환
     */
    public List<PartyListResponseDto> getRecommendedParties(Integer userId, Integer topK) {
        // 1. 사용자 선호 장르 조회
        List<String> preferredGenres = subscribeRepository.findAllByUser_Id(userId)
                .stream()
                .map(subscribe -> subscribe.getGenre().getName())
                .collect(Collectors.toList());

        // 2. 활성/모집 중인 파티의 콘텐츠 ID 추출 (중복 제거)
        List<Integer> activeMovieIds = partyRepository.findAllByIsDeletedFalse()
                .stream()
                .map(Party::getContent)
                .filter(Objects::nonNull)
                .map(Content::getTmdbId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3. AI 서버에 추천 요청
        List<Integer> recommendedMovieIds = aiRecommendationClient.getRecommendations(
                userId,
                preferredGenres,
                activeMovieIds,
                topK != null ? topK : 4);

        // 4. AI 추천 결과를 파티 목록으로 변환
        List<PartyListResponseDto> recommendedParties = new ArrayList<>();

        if (recommendedMovieIds.isEmpty()) {
            // Fallback: AI 서버 장애 시 기본 추천 로직
            log.warn("AI 추천 결과가 비어있음. Fallback 로직 사용");
            return getDefaultRecommendedParties(userId, topK != null ? topK : 4);
        }

        // 추천된 영화 ID로 파티 찾기 (활성 + 모집중 모두, 활성 우선)
        for (Integer movieId : recommendedMovieIds) {
            Optional<Party> bestParty = partyRepository
                    .findAllByContent_TmdbIdAndIsDeletedFalse(movieId)
                    .stream()
                    .filter(party -> !party.getIsPrivate()) // 공개 파티만
                    // .filter(...) // 제거: 비활성화 파티도 포함
                    .max(Comparator
                            .comparing(Party::getIsActive) // 활성화된 파티 우선 (True > False)
                            .thenComparing(Party::getCurrentParticipants) // 참여자 많은 순
                            .thenComparing(Party::getCreatedAt, Comparator.reverseOrder())); // 최근 생성 순

            bestParty.ifPresent(party -> recommendedParties.add(convertToPartyListResponseDto(party, userId))); // 메서드명 수정 (convertToPartyListResponse -> convertToPartyListResponseDto)
        }
        return recommendedParties;
    }

    /**
     * Fallback: AI 서버 장애 시 기본 추천 로직
     * - 최근 24시간 내 생성된 파티 중 참여자 수가 많은 순 (활성 + 모집중)
     */
    private List<PartyListResponseDto> getDefaultRecommendedParties(Integer userId, Integer topK) {
        // 테스트 데이터가 오래되었을 수 있으므로 30일로 확장
        LocalDateTime searchTime = LocalDateTime.now().minusDays(30);

        return partyRepository.findAllByCreatedAtAfterAndIsDeletedFalse(searchTime)
                .stream()
                .filter(party -> !party.getIsPrivate()) // 공개 파티만
                // .filter(...) // 제거
                .sorted(Comparator
                        .comparing(Party::getIsActive, Comparator.reverseOrder()) // 활성 우선
                        .thenComparing(Party::getCurrentParticipants, Comparator.reverseOrder()) // 참여자 많은 순
                        .thenComparing(Party::getCreatedAt, Comparator.reverseOrder())) // 최근 생성 순
                .limit(topK)
                .map(party -> convertToPartyListResponseDto(party, userId))
                .collect(Collectors.toList());
    }

    private PartyListResponseDto convertToPartyListResponse(Party party) {
        // 장르 이름 조회
        List<String> genreNames = party.getContent().getContentGenres().stream()
                .map(contentGenre -> contentGenre.getGenre().getName())
                .collect(Collectors.toList());

        // 호스트 정보 생성
        User host = party.getHost();
        PartyListResponseDto.HostInfo hostInfo = new PartyListResponseDto.HostInfo(
                host.getId(),
                host.getNickname(),
                host.getProfileImageUrl());

        // PartyListResponseDto 생성
        return PartyListResponseDto.from(
                party,
                genreNames,
                null, // 추천 목록에서는 재생 시간 불필요 (또는 0)
                hostInfo);
    }

}
