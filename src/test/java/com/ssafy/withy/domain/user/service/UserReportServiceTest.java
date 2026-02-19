package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.repository.ChatRepository;
import com.ssafy.withy.domain.user.dto.UserReportRequest;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserReportRepository;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

    @InjectMocks
    private UserReportService userReportService;

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("유저 신고 성공")
    void reportUser_Success() {
        // Given
        Integer reporterId = 1;
        Integer chatId = 100;
        UserReportRequest request = new UserReportRequest(chatId, "Reason");

        User reporter = User.builder().id(reporterId).build();
        User reportedUser = User.builder().id(2).build();
        ChatLog chatLog = ChatLog.builder().id(chatId).user(reportedUser).build();

        given(userRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(chatRepository.findById(chatId)).willReturn(Optional.of(chatLog));
        given(userReportRepository.existsByReporterAndChatLog(reporter, chatLog)).willReturn(false);

        // When
        userReportService.reportUser(reporterId, request);

        // Then
        verify(userReportRepository).save(any());
    }

    @Test
    @DisplayName("이미 신고한 채팅이면 ALREADY_REPORTED 예외 발생")
    void reportUser_AlreadyReported() {
        // Given
        Integer reporterId = 1;
        Integer chatId = 100;
        UserReportRequest request = new UserReportRequest(chatId, "Reason");

        User reporter = User.builder().id(reporterId).build();
        ChatLog chatLog = ChatLog.builder().id(chatId).build();

        given(userRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(chatRepository.findById(chatId)).willReturn(Optional.of(chatLog));
        given(userReportRepository.existsByReporterAndChatLog(reporter, chatLog)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> userReportService.reportUser(reporterId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.ALREADY_REPORTED);
    }

    @Test
    @DisplayName("존재하지 않는 채팅이면 CHAT_NOT_FOUND 예외 발생")
    void reportUser_ChatNotFound() {
        // Given
        Integer reporterId = 1;
        UserReportRequest request = new UserReportRequest(999, "Reason");
        User reporter = User.builder().id(reporterId).build();

        given(userRepository.findById(reporterId)).willReturn(Optional.of(reporter));
        given(chatRepository.findById(999)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userReportService.reportUser(reporterId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.CHAT_NOT_FOUND);
    }
}
