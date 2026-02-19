package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.chat.repository.ChatLogRepository;
import com.ssafy.withy.domain.content.dto.GenreDto;
import com.ssafy.withy.domain.content.dto.GenreListResponseDto;
import com.ssafy.withy.domain.content.dto.MyChatLogResponse;
import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.content.repository.WatchHistoryRepository;
import com.ssafy.withy.domain.party.repository.ParticipantRepository;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.dto.*;
import com.ssafy.withy.domain.user.entity.*;
import com.ssafy.withy.domain.user.repository.*;
import com.ssafy.withy.domain.party.repository.PartyCommandLogRepository;
import com.ssafy.withy.domain.dm.repository.DmRoomRepository;
import com.ssafy.withy.domain.dm.entity.DmRoom;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.service.S3Service;
import com.ssafy.withy.global.util.RandomNicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final ChatLogRepository chatLogRepository;
    private final SubscribeRepository subscribeRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserReportRepository userReportRepository;
    private final ParticipantRepository participantRepository;
    private final PartyRepository partyRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final GenreRepository genreRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final DmRoomRepository dmRoomRepository;
    private final PartyCommandLogRepository partyCommandLogRepository;
    private final RandomNicknameGenerator randomNicknameGenerator;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;
    private static final String DEFAULT_PROFILE_PATH = "/profile/default.png";

    public UserProfileResponse getUserProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
    }

    @Transactional
    public void blockUser(Integer blockerId, Integer blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new CustomException(GlobalErrorCode.INVALID_REQUEST, "본인은 차단할 수 없습니다.");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new CustomException(GlobalErrorCode.CONFLICT, "이미 차단된 사용자입니다.");
        }

        UserBlock userBlock = UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        userBlockRepository.save(userBlock);
    }

    @Transactional
    public void unblockUser(Integer blockerId, Integer blockedId) {
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        if (!userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new CustomException(GlobalErrorCode.ENTITY_NOT_FOUND, "차단 목록에 없는 사용자입니다.");
        }

        userBlockRepository.deleteByBlockerAndBlocked(blocker, blocked);
    }

    @Transactional(readOnly = true)
    public List<BlockListResponse> getBlockList(Integer userId) {
        List<UserBlock> blocks = userBlockRepository.findAllByBlockerIdOrderByCreatedAtDesc(userId);

        return blocks.stream()
                .map(block -> BlockListResponse.from(block.getBlocked())) // 혹은 new BlockListResponse(block)
                .toList();
    }

    public UserProfileResponse getMyProfile(Integer userId) {
        return getUserProfile(userId);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Integer userId, UpdateUserRequest request, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 1. 현재 프로필 이미지 URL 가져오기
        String currentProfileImage = user.getProfileImageUrl();

        // 2. 파일이 들어왔는지 확인 (들어왔으면 이미지 변경 로직 수행)
        if (file != null && !file.isEmpty()) {
            // 2-1. 기존 이미지가 '기본 이미지'가 아니라면 S3에서 삭제
            if (currentProfileImage != null && !currentProfileImage.endsWith(DEFAULT_PROFILE_PATH)) {
                try {
                    s3Service.deleteFile(currentProfileImage);
                } catch (Exception e) {
                    // 이미지 삭제 실패는 치명적인 에러가 아니므로 로그만 남기고 진행
//                    log.warn("기존 프로필 이미지 삭제 실패: {}", e.getMessage());
                }
            }
            // 2-2. 새 이미지 업로드 후 URL 교체
            currentProfileImage = s3Service.uploadFile(file, "profile");
        }

        // 3. 유저 정보 업데이트 (닉네임 + 이미지URL)
        user.update(request.nickname(), currentProfileImage);

        return UserProfileResponse.from(user);
    }

    @Transactional
    public void withdrawUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 1. 친구 관계 삭제
        friendshipRepository.deleteAllByUserAOrUserB(user);

        // 2. 차단 관계 삭제
        userBlockRepository.deleteAllByBlockerOrBlocked(user);

        // 3. 신고 내역 삭제
        userReportRepository.deleteAllByReporterOrReported(user);

        participantRepository.deleteAllByUser(user);

        chatLogRepository.deleteAllByUser(user);

        watchHistoryRepository.deleteAllByUserId(userId);

        List<DmRoom> dmRooms = dmRoomRepository.findAllByUserAOrUserB(user, user);
        if(!dmRooms.isEmpty()) {
            dmRoomRepository.deleteAll(dmRooms);
        }

        List<com.ssafy.withy.domain.party.entity.Party> parties = partyRepository.findAllByHost(user);
        if (!parties.isEmpty()) {
            partyRepository.deleteAll(parties);
        }

        partyCommandLogRepository.deleteAllByUser(user);

        userRepository.delete(user);
    }

    public GenreListResponseDto getSubscriptions(Integer userId) {
        List<GenreDto> genreDtos = subscribeRepository.findAllByUser_Id(userId)
                .stream()
                .map(subscribe -> GenreDto.from(subscribe.getGenre()))
                .toList();
        return GenreListResponseDto.from(genreDtos);
    }

    @Transactional
    public void unsubscribeGenre(Integer userId, Integer genreId) {
        if (!subscribeRepository.existsByUser_IdAndGenre_Id(userId, genreId)) {
            throw new CustomException(GlobalErrorCode.ENTITY_NOT_FOUND, "구독 정보를 찾을 수 없습니다.");
        }
        subscribeRepository.deleteByUser_IdAndGenre_Id(userId, genreId);
    }

    @Transactional
    public BulkSubscribeUpdateResponse updateSubscriptions(
            Integer userId,
            List<Integer> genreIds) {
        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 2. 새로운 장르 ID 유효성 검증
        List<Genre> validGenres = List.of();
        if (!genreIds.isEmpty()) {
            validGenres = genreRepository.findByIdIn(genreIds);

            // 존재하지 않는 장르 ID 확인
            if (validGenres.size() != genreIds.size()) {
                List<Integer> foundIds = validGenres.stream()
                        .map(Genre::getId)
                        .toList();
                List<Integer> invalidIds = genreIds.stream()
                        .filter(id -> !foundIds.contains(id))
                        .toList();
                throw new CustomException(
                        GlobalErrorCode.INVALID_REQUEST,
                        "존재하지 않는 장르 ID가 포함되어 있습니다: " + invalidIds);
            }
        }

        // 3. 기존 구독 전체 삭제
        subscribeRepository.deleteAllByUserId(userId);

        // 4. 새로운 구독 일괄 생성 (빈 배열이면 스킵)
        if (!genreIds.isEmpty()) {
            List<Subscribe> newSubscribes = validGenres.stream()
                    .map(genre -> Subscribe.builder()
                            .user(user)
                            .genre(genre)
                            .build())
                    .toList();

            subscribeRepository.saveAll(newSubscribes); // Batch Insert
        }

        // 5. 업데이트된 구독 목록 반환
        List<GenreDto> genreDtos = validGenres.stream()
                .map(GenreDto::from)
                .toList();

        return BulkSubscribeUpdateResponse.from(genreDtos);
    }

    public List<MyChatLogResponse> getMyChatLogs(Integer userId, Pageable pageable) {
        var chatLogs = chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return chatLogs.stream()
                .map(chatLog -> new MyChatLogResponse(
                        chatLog.getId(),
                        chatLog.getMessage(),
                        chatLog.getParty().getTitle(),
                        chatLog.getParty().getId(),
                        chatLog.getCreatedAt().toString()))
                .toList();
    }

    // --- 1. 중복 체크 ---
    public CheckDuplicateResponse checkEmailDuplicate(String email) {
        return new CheckDuplicateResponse(userRepository.existsByEmail(email));
    }

    public CheckDuplicateResponse checkNicknameDuplicate(String nickname) {
        return new CheckDuplicateResponse(userRepository.existsByNickname(nickname));
    }

    // --- 2. 랜덤 닉네임 생성 ---
    public RandomNicknameResponse generateRandomNickname() {
        return getRandomNickname();
    }

    // --- 3. 닉네임 변경 ---
    @Transactional
    public void updateNickname(Integer userId, String newNickname) {
        // 중복 검사
        if (userRepository.existsByNickname(newNickname)) {
            throw new CustomException(GlobalErrorCode.ALREADY_EXIST_NICKNAME);
        }

        User user = getUserOrThrow(userId);
        user.updateNickname(newNickname);
    }

    // --- 4. 장르 구독 (Preferences) ---
    @Transactional
    public void updateGenrePreferences(Integer userId, List<Integer> genreIds) {
        User user = getUserOrThrow(userId);

        // 1. 요청된 장르들이 진짜 DB에 있는지 조회 (WHERE id IN (...))
        List<Genre> requestGenres = genreRepository.findByIdIn(genreIds);

        // 요청한 ID 개수랑 실제 조회된 개수가 다르면? -> 이상한 ID가 섞여있음
        if (requestGenres.size() != genreIds.size()) {
            throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        }

        // 2. 이미 유저가 구독 중인 장르 ID 목록 가져오기
        List<Integer> existingGenreIds = subscribeRepository.findGenreIdsByUserId(userId);

        // 3.] 요청 목록 중, 이미 구독 중인 건 빼고(Filter) 새로운 것만 리스트로 만듦
        List<Subscribe> newSubscribes = requestGenres.stream()
                .filter(genre -> !existingGenreIds.contains(genre.getId())) // 중복 제거
                .map(genre -> Subscribe.builder()
                        .user(user)
                        .genre(genre)
                        .build())
                .toList();

        // 4. 새로운 게 있을 때만 저장
        if (!newSubscribes.isEmpty()) {
            subscribeRepository.saveAll(newSubscribes);
        }
    }

    /**
     * 닉네임으로 유저 검색 (정확히 일치)
     */
    @Transactional(readOnly = true)
    public UserSearchResponse searchUserByNickname(String nickname, Integer currentUserId) {
        // 1. 닉네임으로 대상 유저 찾기
        User targetUser = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 2. 나와 그 사람 사이의 친구 관계 확인
        boolean isFriend = false;
        if (!targetUser.getId().equals(currentUserId)) {
            isFriend = friendshipRepository.existsFriendship(currentUserId, targetUser.getId());
        }

        boolean isSent = friendRequestRepository.existsByRequester_IdAndReceiver_IdAndStatus(
                currentUserId,
                targetUser.getId(),
                FriendRequestStatus.PENDING
        );

        // 3. DTO 변환해서 반환
        return UserSearchResponse.from(targetUser, isFriend, isSent);
    }

    @Transactional
    public void changePassword(Integer userId, PasswordChangeRequest request) {
        User user = getUserOrThrow(userId);

        // 1. 소셜 로그인 유저인지 확인
        if (user.getLoginType() != LoginType.LOCAL) {
            throw new CustomException(GlobalErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        // 2. 현재 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(GlobalErrorCode.PASSWORD_NOT_MATCH);
        }

        // 3. 기존 비밀번호와 새 비밀번호가 같은지 체크
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new CustomException(GlobalErrorCode.SAME_PASSWORD);
        }

        // 4. 새 비밀번호 암호화 및 변경
        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedPassword);
    }

    @Transactional
    public void updatePreferredLanguage(Integer userId, String language) {
        User user = getUserOrThrow(userId);

        user.updatePreferredLanguage(language);
    }

    private User getUserOrThrow(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));
    }

    public RandomNicknameResponse getRandomNickname () {
        String nickname;
        int maxRetry = 10;

        do {
            nickname = randomNicknameGenerator.generate();
            maxRetry--;

            if (maxRetry < 0) {
                // 10번 돌려도 중복이면 뭔가 잘못된 거임 (단어 풀 부족 등)
                throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }
        } while (userRepository.existsByNickname(nickname));

        // 통과했으면 리턴
        return new RandomNicknameResponse(nickname);
    }

    @Transactional
    public void completeOnboarding(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        // 이미 봤으면 굳이 업데이트 쿼리 날릴 필요 없으니 체크 (최적화)
        if (!user.isOnboardingComplete()) {
            user.completeOnboarding();
        }
    }
}
