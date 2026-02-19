package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.repository.ChatRepository;
import com.ssafy.withy.domain.user.dto.UserReportRequest;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.entity.UserReport;
import com.ssafy.withy.domain.user.repository.UserReportRepository;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserReportService {

    private final UserReportRepository userReportRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional
    public void reportUser(Integer reporterId, UserReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.USER_NOT_FOUND));

        ChatLog chatLog = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.CHAT_NOT_FOUND));

        if (userReportRepository.existsByReporterAndChatLog(reporter, chatLog)) {
            throw new CustomException(GlobalErrorCode.ALREADY_REPORTED);
        }

        UserReport userReport = UserReport.builder()
                .reporter(reporter)
                .reported(chatLog.getUser())
                .chatLog(chatLog)
                .reason(request.reason())
                .build();

        userReportRepository.save(userReport);
    }
}
