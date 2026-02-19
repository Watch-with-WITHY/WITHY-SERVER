package com.ssafy.withy.global.config;

import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import com.ssafy.withy.domain.user.entity.Role;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PartyRepository partyRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 1. 테스트 유저 생성 (ID: 101)
            // auto-increment라 ID 강제 설정이 어려울 수 있으니, 없으면 만들고 ID를 확인해야 함
            // 하지만 테스트 편의를 위해 일단 save 후 로그로 ID 확인
            // 1. 테스트 유저 1 생성 (ID 확인용 로그 포함)
            if (!userRepository.existsByEmail("test@test.com")) {
                User user1 = User.builder()
                        .email("test@test.com")
                        .nickname("테스트유저")
                        .loginType(com.ssafy.withy.domain.user.entity.LoginType.LOCAL)
                        .password("password1234")
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                User savedUser1 = userRepository.save(user1);
                System.out.println(">>> [Init] Test User 1 Created. User ID: " + savedUser1.getId());
            }

            // 2. 테스트 유저 2 생성
            if (!userRepository.existsByEmail("test2@test.com")) {
                User user2 = User.builder()
                        .email("test2@test.com")
                        .nickname("테스트유저2")
                        .loginType(com.ssafy.withy.domain.user.entity.LoginType.LOCAL)
                        .password("password1234")
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                User savedUser2 = userRepository.save(user2);
                System.out.println(">>> [Init] Test User 2 Created. User ID: " + savedUser2.getId());
            }

            // 2. 테스트 파티 생성
            if (partyRepository.count() == 0) {
                User user3 = User.builder()
                        .email("test@test.com")
                        .nickname("테스트유저")
                        .loginType(com.ssafy.withy.domain.user.entity.LoginType.LOCAL)
                        .password("password1234")
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                User savedUser1 = userRepository.save(user3);

                Party party = Party.builder()
                        .title("테스트 파티")
                        .host(user3)
                        .platform(com.ssafy.withy.domain.party.entity.PlatformType.OTT)
                        .scheduledActiveTime(LocalDateTime.now().plusHours(1))
                        .currentParticipants(1)
                        .maxParticipants(4)
                        .isActive(true)
                        .isPrivate(false)
                        .build();
                Party savedParty = partyRepository.save(party);
                System.out.println(">>> [Init] Test Party Created. Party ID: " + savedParty.getId());
            }
        };
    }
}
