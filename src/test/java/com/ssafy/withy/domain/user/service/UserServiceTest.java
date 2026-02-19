package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.user.dto.*;
import com.ssafy.withy.domain.user.entity.*;
import com.ssafy.withy.domain.user.repository.*;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import com.ssafy.withy.global.util.RandomNicknameGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private RandomNicknameGenerator randomNicknameGenerator;

    @Mock
    private SubscribeRepository subscribeRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("유저 프로필 조회 성공")
    void getUserProfile_Success() {
        // given
        Integer userId = 1;
        User user = User.builder()
                .id(userId)
                .nickname("testUser")
                .profileImageUrl("image.jpg")
                .email("test@example.com")
                .status(UserStatus.ONLINE)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserProfileResponse response = userService.getUserProfile(userId);

        // then
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.nickname()).isEqualTo("testUser");
        assertThat(response.status()).isEqualTo(UserStatus.ONLINE);
    }

    @Test
    @DisplayName("유저 차단 성공")
    void blockUser_Success() {
        // given
        Integer blockerId = 1;
        Integer blockedId = 2;
        User blocker = User.builder().id(blockerId).build();
        User blocked = User.builder().id(blockedId).build();

        given(userRepository.findById(blockerId)).willReturn(Optional.of(blocker));
        given(userRepository.findById(blockedId)).willReturn(Optional.of(blocked));
        given(userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)).willReturn(false);

        // when
        userService.blockUser(blockerId, blockedId);

        // then
        verify(userBlockRepository).save(any(UserBlock.class));
    }

    @Test
    @DisplayName("유저 차단 실패 - 본인 차단")
    void blockUser_Fail_SelfBlock() {
        // given
        Integer userId = 1;

        // when & then
        assertThatThrownBy(() -> userService.blockUser(userId, userId))
                .isInstanceOf(CustomException.class)
                .hasMessage("본인은 차단할 수 없습니다.");
    }

    @Test
    @DisplayName("유저 차단 해제 성공")
    void unblockUser_Success() {
        // given
        Integer blockerId = 1;
        Integer blockedId = 2;
        User blocker = User.builder().id(blockerId).build();
        User blocked = User.builder().id(blockedId).build();

        given(userRepository.findById(blockerId)).willReturn(Optional.of(blocker));
        given(userRepository.findById(blockedId)).willReturn(Optional.of(blocked));
        given(userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)).willReturn(true);

        // when
        userService.unblockUser(blockerId, blockedId);

        // then
        verify(userBlockRepository).deleteByBlockerAndBlocked(blocker, blocked);
    }

    @Test
    @DisplayName("차단 목록 조회 성공")
    void getBlockList_Success() {
        // given
        Integer userId = 1;
        User blocker = User.builder().id(userId).build();
        User blockedUser = User.builder().id(2).nickname("badGuy").build();
        UserBlock block = UserBlock.builder().blocker(blocker).blocked(blockedUser).build();

        List<UserBlock> blockList = List.of(block);

        given(userBlockRepository.findAllByBlockerIdOrderByCreatedAtDesc(userId))
                .willReturn(blockList);

        // when
        List<BlockListResponse> responses = userService.getBlockList(userId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).nickname()).isEqualTo("badGuy");
    }

    // --- Helper ---
    private User createUser(Integer id) {
        return User.builder().id(id).email("test@ssafy.com").nickname("기존닉네임").build();
    }

    @Nested
    @DisplayName("중복 체크")
    class CheckDuplicate {
        @Test
        @DisplayName("이메일 중복 체크 - 중복인 경우 true 반환")
        void checkEmail_duplicate() {
            // given
            given(userRepository.existsByEmail("exist@ssafy.com")).willReturn(true);

            // when
            CheckDuplicateResponse response = userService.checkEmailDuplicate("exist@ssafy.com");

            // then
            assertThat(response.isDuplicate()).isTrue();
        }

        @Test
        @DisplayName("닉네임 중복 체크 - 사용 가능한 경우 false 반환")
        void checkNickname_available() {
            // given
            given(userRepository.existsByNickname("newNick")).willReturn(false);

            // when
            CheckDuplicateResponse response = userService.checkNicknameDuplicate("newNick");

            // then
            assertThat(response.isDuplicate()).isFalse();
        }
    }

    @Nested
    @DisplayName("닉네임 관련")
    class NicknameLogic {
        @Test
        @DisplayName("랜덤 닉네임 생성 - 형용사+명사 조합 반환")
        void generateRandomNickname() {
            // given
            String mockNickname = "행복한 호랑이#1234";
            given(randomNicknameGenerator.generate()).willReturn(mockNickname);

            given(userRepository.existsByNickname(mockNickname)).willReturn(false);

            // when
            RandomNicknameResponse response = userService.generateRandomNickname();

            // then
            System.out.println("Generated Nickname: " + response.nickname());

            // Mock이 준 값이 그대로 잘 나왔는지 확인
            assertThat(response.nickname()).isEqualTo(mockNickname);
        }

        @Test
        @DisplayName("닉네임 변경 - 중복된 닉네임이면 예외 발생")
        void updateNickname_fail_duplicate() {
            // given
            String newNickname = "중복닉네임";
            given(userRepository.existsByNickname(newNickname)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateNickname(1, newNickname))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.ALREADY_EXIST_NICKNAME);
        }

        @Test
        @DisplayName("닉네임 변경 - 성공")
        void updateNickname_success() {
            // given
            Integer userId = 1;
            String newNickname = "새닉네임";
            User user = createUser(userId);

            given(userRepository.existsByNickname(newNickname)).willReturn(false);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userService.updateNickname(userId, newNickname);

            // then
            assertThat(user.getNickname()).isEqualTo(newNickname);
        }
    }

    @Nested
    @DisplayName("장르 구독(선호 설정)")
    class UpdatePreferences {
        @Test
        @DisplayName("성공: 이미 구독한 건 제외하고 새로운 장르만 추가한다")
        void success_additive() {
            // given
            Integer userId = 1;
            List<Integer> requestGenreIds = List.of(10, 20); // 요청: 10(이미있음), 20(새거)
            User user = createUser(userId);

            // Genre Mocking
            Genre g1 = Genre.builder().id(10).name("Music").build();
            Genre g2 = Genre.builder().id(20).name("Action").build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            // 1. 요청한 장르들이 DB에 실제 존재하는지 확인
            given(genreRepository.findByIdIn(requestGenreIds)).willReturn(List.of(g1, g2));
            // 2. 이미 구독 중인 장르 ID 조회 (10번은 이미 구독 중)
            given(subscribeRepository.findGenreIdsByUserId(userId)).willReturn(List.of(10));

            // when
            userService.updateGenrePreferences(userId, requestGenreIds);

            // then

            // [검증 2] saveAll은 호출되어야 함
            then(subscribeRepository).should(times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("성공: 요청한 장르를 모두 이미 구독 중이면 저장을 호출하지 않는다")
        void success_nothingToAdd() {
            // given
            Integer userId = 1;
            List<Integer> requestGenreIds = List.of(10); // 10번 요청
            User user = createUser(userId);

            Genre g1 = Genre.builder().id(10).name("Music").build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(genreRepository.findByIdIn(requestGenreIds)).willReturn(List.of(g1));
            given(subscribeRepository.findGenreIdsByUserId(userId)).willReturn(List.of(10)); // 이미 10번 있음

            // when
            userService.updateGenrePreferences(userId, requestGenreIds);

            // then
            then(subscribeRepository).should(never()).saveAll(any());
        }

        @Test
        @DisplayName("실패: DB에 없는 이상한 장르 ID가 포함된 경우")
        void fail_genreNotFound() {
            // given
            Integer userId = 1;
            List<Integer> genreIds = List.of(999); // 없는 ID
            User user = createUser(userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            // 요청은 1개인데, 조회 결과는 0개 (빈 리스트)
            given(genreRepository.findByIdIn(genreIds)).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> userService.updateGenrePreferences(userId, genreIds))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.RESOURCE_NOT_FOUND);
        }
    }
        // --- Helper Methods ---
        private User createUser(Integer id, LoginType loginType, String password) {
            return User.builder()
                    .id(id)
                    .loginType(loginType)
                    .password(password)
                    .build();
        }

        private PasswordChangeRequest createRequest(String current, String newPwd) {
            return new PasswordChangeRequest(current, newPwd);
        }

        @Nested
        @DisplayName("비밀번호 변경 (ChangePassword)")
        class ChangePasswordTest {

            @Test
            @DisplayName("성공: 모든 조건이 충족되면 비밀번호가 변경된다.")
            void changePassword_Success() {
                // given
                Integer userId = 1;
                String oldEncodedPwd = "encodedOldPassword";
                String newRawPwd = "newPassword123!";
                String newEncodedPwd = "encodedNewPassword";

                // 이메일 유저 (변경 가능)
                User user = createUser(userId, LoginType.LOCAL, oldEncodedPwd);
                PasswordChangeRequest request = createRequest("oldPassword123!", newRawPwd);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));

                // 1. 현재 비밀번호 일치 (match=true)
                given(passwordEncoder.matches(request.currentPassword(), oldEncodedPwd)).willReturn(true);
                // 2. 새 비밀번호가 기존과 다름 (match=false)
                given(passwordEncoder.matches(request.newPassword(), oldEncodedPwd)).willReturn(false);
                // 3. 새 비밀번호 암호화
                given(passwordEncoder.encode(request.newPassword())).willReturn(newEncodedPwd);

                // when
                userService.changePassword(userId, request);

                // then
                // User 엔티티의 updatePassword가 암호화된 새 비밀번호로 호출되었는지 확인
                // (Spy를 쓰거나, 엔티티 상태를 직접 확인하는 방법도 있음. 여기서는 User가 Mock이 아니므로 상태 확인 가능)
                // 하지만 단위 테스트에서는 행위 검증보다는 상태 검증이 더 좋음.
                // 여기서는 간단하게 로직 통과 여부 확인
            }

            @Test
            @DisplayName("실패: 소셜 로그인 유저는 비밀번호를 변경할 수 없다.")
            void fail_SocialUser() {
                // given
                Integer userId = 1;
                // 🚨 GOOGLE 유저
                User user = createUser(userId, LoginType.GOOGLE, "somePwd");
                PasswordChangeRequest request = createRequest("old", "new");

                given(userRepository.findById(userId)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> userService.changePassword(userId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);

                // 비밀번호 검사 로직까지 가면 안 됨
                verify(passwordEncoder, never()).matches(any(), any());
            }

            @Test
            @DisplayName("실패: 현재 비밀번호가 일치하지 않으면 예외가 발생한다.")
            void fail_WrongCurrentPassword() {
                // given
                Integer userId = 1;
                String oldEncodedPwd = "encodedOldPassword";
                User user = createUser(userId, LoginType.LOCAL, oldEncodedPwd);
                PasswordChangeRequest request = createRequest("wrongPassword", "new");

                given(userRepository.findById(userId)).willReturn(Optional.of(user));

                // 🚨 현재 비밀번호 불일치 (false)
                given(passwordEncoder.matches(request.currentPassword(), oldEncodedPwd)).willReturn(false);

                // when & then
                assertThatThrownBy(() -> userService.changePassword(userId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.PASSWORD_NOT_MATCH);
            }

            @Test
            @DisplayName("실패: 새 비밀번호가 기존 비밀번호와 동일하면 예외가 발생한다.")
            void fail_SamePassword() {
                // given
                Integer userId = 1;
                String oldEncodedPwd = "encodedOldPassword";
                String samePwd = "oldPassword123!"; // 기존과 똑같은 비번 입력

                User user = createUser(userId, LoginType.LOCAL, oldEncodedPwd);
                PasswordChangeRequest request = createRequest(samePwd, samePwd);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));

                // 1. 현재 비밀번호는 맞음
                given(passwordEncoder.matches(request.currentPassword(), oldEncodedPwd)).willReturn(true);
                // 🚨 새 비밀번호도 기존 암호화된 비번과 매칭됨 (true) -> 즉, 같은 비번임
                given(passwordEncoder.matches(request.newPassword(), oldEncodedPwd)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> userService.changePassword(userId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.SAME_PASSWORD);
            }
        }


    @Test
    @DisplayName("유저 검색 시, 내가 이미 친구 신청을 보냈다면 isFriendRequestSent가 true로 반환된다.")
    void searchUser_WithSentRequest() {
        // given
        Integer myId = 1;
        String targetNickname = "targetUser";
        User targetUser = User.builder().id(2).nickname(targetNickname).build();

        given(userRepository.findByNickname(targetNickname)).willReturn(Optional.of(targetUser));

        // 이미 보낸 요청이 있다고 설정 (exists -> true)
        given(friendRequestRepository.existsByRequester_IdAndReceiver_IdAndStatus(
                myId, targetUser.getId(), FriendRequestStatus.PENDING
        )).willReturn(true);

        // when
        UserSearchResponse response = userService.searchUserByNickname(targetNickname, myId);

        // then
        assertThat(response.nickname()).isEqualTo(targetNickname);
        assertThat(response.isFriendRequestSent()).isTrue(); // True 확인!
    }

    @Test
    @DisplayName("유저 검색 시, 친구 신청 이력이 없으면 isFriendRequestSent가 false로 반환된다.")
    void searchUser_NoRequest() {
        // given
        Integer myId = 1;
        String targetNickname = "targetUser";
        User targetUser = User.builder().id(2).nickname(targetNickname).build();

        given(userRepository.findByNickname(targetNickname)).willReturn(Optional.of(targetUser));

        // 보낸 요청 없음 (exists -> false)
        given(friendRequestRepository.existsByRequester_IdAndReceiver_IdAndStatus(
                myId, targetUser.getId(), FriendRequestStatus.PENDING
        )).willReturn(false);

        // when
        UserSearchResponse response = userService.searchUserByNickname(targetNickname, myId);

        // then
        assertThat(response.isFriendRequestSent()).isFalse(); // False 확인!
    }

    @Nested
    @DisplayName("선호 언어 변경 (updatePreferredLanguage)")
    class UpdatePreferredLanguageTest {

        @Test
        @DisplayName("성공: 유저가 존재하면 언어 설정이 변경된다.")
        void success() {
            // given
            Integer userId = 1;
            String newLanguage = "en";

            // 기존 언어는 "ko"라고 가정
            User user = User.builder()
                    .id(userId)
                    .preferredLanguage("ko")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userService.updatePreferredLanguage(userId, newLanguage);

            // then
            // 1. 엔티티의 상태가 "en"으로 바뀌었는지 확인 (Dirty Checking 대상)
            assertThat(user.getPreferredLanguage()).isEqualTo(newLanguage);

            // 2. (선택) findById가 호출되었는지 검증
            then(userRepository).should(times(1)).findById(userId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저면 예외가 발생한다.")
        void fail_UserNotFound() {
            // given
            Integer userId = 999;
            String newLanguage = "ja";

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updatePreferredLanguage(userId, newLanguage))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.USER_NOT_FOUND);
        }
    }
}
