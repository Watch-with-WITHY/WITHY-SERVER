package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.entity.MessageType;
import com.ssafy.withy.domain.chat.repository.ChatLogRepository;
import com.ssafy.withy.domain.content.dto.MyChatLogResponse;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceChatLogTest {

    @Mock
    private ChatLogRepository chatLogRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 채팅 로그 조회 - 성공")
    void getMyChatLogs_success() {
        // given
        Integer userId = 1;
        Pageable pageable = PageRequest.of(0, 20);

        User user = User.builder().id(userId).nickname("테스터").build();
        Party party1 = Party.builder().id(10).title("오징어 게임 같이 봐요").build();
        Party party2 = Party.builder().id(11).title("지옥 시즌2 기대").build();

        LocalDateTime time1 = LocalDateTime.of(2024, 5, 20, 10, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 5, 20, 11, 30);

        ChatLog chatLog1 = ChatLog.builder()
                .id(100)
                .user(user)
                .party(party1)
                .message("안녕하세요! 반갑습니다.")
                .messageType(MessageType.TEXT)
                .isSpoiler(false)
                .isProfanity(false)
                .createdAt(time1)
                .build();

        ChatLog chatLog2 = ChatLog.builder()
                .id(101)
                .user(user)
                .party(party2)
                .message("이 드라마 정말 재밌어요!")
                .messageType(MessageType.TEXT)
                .isSpoiler(false)
                .isProfanity(false)
                .createdAt(time2)
                .build();

        given(chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(List.of(chatLog2, chatLog1)); // 최신순 정렬

        // when
        List<MyChatLogResponse> responses = userService.getMyChatLogs(userId, pageable);

        // then
        assertThat(responses).hasSize(2);

        // 첫 번째 (최신) 로그 검증
        MyChatLogResponse response1 = responses.get(0);
        assertThat(response1.id()).isEqualTo(101);
        assertThat(response1.message()).isEqualTo("이 드라마 정말 재밌어요!");
        assertThat(response1.partyTitle()).isEqualTo("지옥 시즌2 기대");
        assertThat(response1.partyId()).isEqualTo(11);

        // 두 번째 로그 검증
        MyChatLogResponse response2 = responses.get(1);
        assertThat(response2.id()).isEqualTo(100);
        assertThat(response2.message()).isEqualTo("안녕하세요! 반갑습니다.");
        assertThat(response2.partyId()).isEqualTo(10);
    }

    @Test
    @DisplayName("내 채팅 로그 조회 - 빈 리스트")
    void getMyChatLogs_emptyList() {
        // given
        Integer userId = 1;
        Pageable pageable = PageRequest.of(0, 20);

        given(chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(List.of());

        // when
        List<MyChatLogResponse> responses = userService.getMyChatLogs(userId, pageable);

        // then
        assertThat(responses).isEmpty();
    }
}
