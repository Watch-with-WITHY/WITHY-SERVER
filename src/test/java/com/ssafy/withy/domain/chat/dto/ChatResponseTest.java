package com.ssafy.withy.domain.chat.dto;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.entity.MessageType;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatResponseTest {

    @Test
    @DisplayName("ChatLog 엔티티로부터 변환 시 닉네임이 올바르게 매핑되어야 한다")
    void from_ShouldMapNicknameCorrectly() {
        // given
        User user = User.builder()
                .id(100)
                .nickname("테스트유저")
                .build();

        Party party = Party.builder()
                .id(10)
                .build();

        ChatLog chatLog = ChatLog.builder()
                .id(1)
                .party(party)
                .user(user)
                .message("테스트 메시지")
                .messageType(MessageType.TEXT)
                .isSpoiler(false)
                .isProfanity(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        ChatResponse response = ChatResponse.from(chatLog);

        // then
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getPartyId()).isEqualTo(10);
        assertThat(response.getUserId()).isEqualTo(100);
        assertThat(response.getNickname()).isEqualTo("테스트유저");
        assertThat(response.getContent()).isEqualTo("테스트 메시지");
        assertThat(response.getContent()).isEqualTo("테스트 메시지");
        // originalContent 필드가 삭제되었으므로 해당 검증 제거
    }

    @Test
    @DisplayName("빌더 패턴을 통해 content를 설정할 수 있어야 한다")
    void builder_ShouldSetContentCorrectly() {
        ChatResponse response = ChatResponse.builder()
                .content("스포일러가 포함된 메시지입니다.")
                .build();

        assertThat(response.getContent()).isEqualTo("스포일러가 포함된 메시지입니다.");
    }
}
