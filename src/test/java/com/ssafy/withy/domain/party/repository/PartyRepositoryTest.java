package com.ssafy.withy.domain.party.repository;

import com.ssafy.withy.domain.content.entity.*;
import com.ssafy.withy.domain.party.entity.*;
import com.ssafy.withy.domain.user.entity.*;
import com.ssafy.withy.domain.chat.entity.ChatLog;
import com.ssafy.withy.domain.chat.entity.MessageType;
import com.ssafy.withy.domain.chat.repository.ChatLogRepository;
import com.ssafy.withy.global.config.JpaConfig;
import com.ssafy.withy.global.config.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class })
class PartyRepositoryTest {

        @Autowired
        private PartyRepository partyRepository;

        @Autowired
        private ChatLogRepository chatLogRepository;

        @Autowired
        private TestEntityManager em;

        private Genre genreYoutubeEnt;
        private Genre genreMovieHorror; // OTT TMDB 27

        @BeforeEach
        void setUp() {
                // 테스트용 장르 생성
                genreYoutubeEnt = Genre.builder()
                                .code(24)
                                .type(PlatformType.YOUTUBE)
                                .name("Entertainment")
                                .createdAt(LocalDateTime.now())
                                .build();

                // ID 충돌 테스트용 (ID 27)
                Genre genreYoutubeEdu = Genre.builder()
                                .code(27)
                                .type(PlatformType.YOUTUBE)
                                .name("Education")
                                .createdAt(LocalDateTime.now())
                                .build();

                genreMovieHorror = Genre.builder()
                                .code(27)
                                .type(PlatformType.OTT)
                                .name("Horror")
                                .createdAt(LocalDateTime.now())
                                .build();

                em.persist(genreYoutubeEnt);
                em.persist(genreYoutubeEdu);
                em.persist(genreMovieHorror);

                // 테스트용 콘텐츠 생성
                Content contentYoutube = Content.builder()
                                .title("Funny Video")
                                .mediaType(MediaType.YOUTUBE)
                                .externalId("vid1")
                                .build();

                Content contentMovie = Content.builder()
                                .title("Scary Movie")
                                .mediaType(MediaType.MOVIE)
                                .originalLanguage("en")
                                .tmdbId(1001)
                                .build();

                Content contentKoreanMovie = Content.builder()
                                .title("Korean Action")
                                .mediaType(MediaType.MOVIE)
                                .originalLanguage("ko")
                                .tmdbId(1002)
                                .build();

                em.persist(contentYoutube);
                em.persist(contentMovie);
                em.persist(contentKoreanMovie);

                // 콘텐츠-장르 연결
                em.persist(ContentGenre.builder().content(contentYoutube).genre(genreYoutubeEnt).build());
                em.persist(ContentGenre.builder().content(contentMovie).genre(genreMovieHorror).build()); // 영화 - 공포 장르
                                                                                                          // 연결

                // 테스트용 호스트 생성
                User host = User.builder()
                                .email("host@test.com")
                                .nickname("HostUser")
                                .loginType(LoginType.LOCAL)
                                .role(Role.USER)
                                .isActive(true)
                                .status(UserStatus.ONLINE)
                                .build();
                em.persist(host);

                // 테스트용 파티 생성
                Party partyYoutube = Party.builder()
                                .title("Youtube Party")
                                .content(contentYoutube)
                                .platform(PlatformType.YOUTUBE)
                                .isActive(true)
                                .isPrivate(false)
                                .maxParticipants(4)
                                .currentParticipants(1)
                                .scheduledActiveTime(LocalDateTime.now().plusHours(1))
                                .host(host)
                                .build();

                Party partyMovie = Party.builder()
                                .title("Horror Movie Party")
                                .content(contentMovie)
                                .platform(PlatformType.OTT)
                                .isActive(true)
                                .isPrivate(false)
                                .maxParticipants(4)
                                .currentParticipants(1)
                                .scheduledActiveTime(LocalDateTime.now().plusHours(2))
                                .host(host)
                                .build();

                Party partyKoreanForign = Party.builder()
                                .title("Korean Movie Party")
                                .content(contentKoreanMovie)
                                .platform(PlatformType.OTT)
                                .isActive(true)
                                .isPrivate(false)
                                .maxParticipants(4)
                                .currentParticipants(1)
                                .scheduledActiveTime(LocalDateTime.now().plusHours(3))
                                .host(host)
                                .build();

                em.persist(partyYoutube);
                em.persist(partyMovie);
                em.persist(partyKoreanForign);

                em.flush();
                em.clear();
        }

        @Test
        @DisplayName("유튜브 카테고리 필터링 조회")
        void searchYoutubeCategory() {
                // When: Platform=YOUTUBE, Category=24 (Ent)
                Page<Party> result = partyRepository.searchParties(PlatformType.YOUTUBE, "24", null, PageRequest.of(0, 10));

                // Then
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getTitle()).isEqualTo("Youtube Party");
        }

        @Test
        @DisplayName("OTT 장르 ID 필터링 (ID 27 중복 테스트)")
        void searchOttGenreCollision() {
                // When: Platform=OTT, Category=27 (Horror)
                Page<Party> result = partyRepository.searchParties(PlatformType.OTT, "27", null, PageRequest.of(0, 10));

                // Then
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getTitle()).isEqualTo("Horror Movie Party");
        }

        @Test
        @DisplayName("OTT 한국 영화 필터링")
        void searchKoreanMovie() {
                // When: Platform=OTT, Category=KO
                Page<Party> result = partyRepository.searchParties(PlatformType.OTT, "KO", null, PageRequest.of(0, 10));

                // Then
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getTitle()).isEqualTo("Korean Movie Party");
        }

        @Test
        @DisplayName("OTT 범죄 장르(Code 80) 필터링 - 사용자 제보 재현")
        void searchOttCrimeGenre() {
                // Given
                Genre genreCrime = Genre.builder()
                                .code(80)
                                .type(PlatformType.OTT)
                                .name("범죄")
                                .createdAt(LocalDateTime.now())
                                .build();
                em.persist(genreCrime);

                Content contentCrime = Content.builder()
                                .title("Crime Movie")
                                .mediaType(MediaType.TV) // 수리남(TV) 예시
                                .externalId("crime123")
                                .build();
                em.persist(contentCrime);

                em.persist(ContentGenre.builder().content(contentCrime).genre(genreCrime).build());

                User host = User.builder()
                                .email("host2@test.com")
                                .nickname("HostUser2")
                                .loginType(LoginType.LOCAL)
                                .role(Role.USER)
                                .isActive(true)
                                .status(UserStatus.ONLINE)
                                .build();
                em.persist(host);

                Party partyCrime = Party.builder()
                                .title("Crime Party")
                                .content(contentCrime)
                                .platform(PlatformType.OTT)
                                .isActive(true)
                                .isPrivate(false) // 필수 필드 추가
                                .host(host)
                                .maxParticipants(4)
                                .currentParticipants(1)
                                .scheduledActiveTime(LocalDateTime.now().plusHours(1))
                                .build();
                em.persist(partyCrime);

                em.flush();
                em.clear();

                // When: Platform=OTT, Category=80 (Crime)
                Page<Party> result = partyRepository.searchParties(PlatformType.OTT, "80", null, PageRequest.of(0, 10));

                // Then
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getTitle()).isEqualTo("Crime Party");
        }

        @Test
        @DisplayName("파티 삭제 시 채팅 로그도 함께 삭제되는지 검증 (Cascade.REMOVE)")
        void deleteParty_cascadesToChatLogs() {
                // Given: 파티 생성 및 채팅 로그 추가
                User host = User.builder()
                        .email("host_chat@test.com")
                        .nickname("HostChat")
                        .loginType(LoginType.LOCAL)
                        .role(Role.USER)
                        .isActive(true)
                        .status(UserStatus.ONLINE)
                        .build();
                em.persist(host);

                Party party = Party.builder()
                        .title("Chat Test Party")
                        .platform(PlatformType.OTT)
                        .isActive(true)
                        .isPrivate(false)
                        .maxParticipants(4)
                        .currentParticipants(1)
                        .scheduledActiveTime(LocalDateTime.now().plusHours(1))
                        .host(host)
                        .build();
                em.persist(party);

                // 명시적으로 Content 설정 (NullPointerException 방지)
                Content content = Content.builder()
                        .title("Chat Test Content")
                        .mediaType(MediaType.MOVIE)
                        .tmdbId(9999)
                        .build();
                em.persist(content);
                party.update(party.getTitle(), party.getMaxParticipants(), party.getIsPrivate(), party.getPassword(), content, party.getPlatform());


                ChatLog chatLog = ChatLog.builder()
                        .party(party)
                        .user(host)
                        .message("Hello World")
                        .messageType(MessageType.TEXT)
                        .isSpoiler(false)
                        .isProfanity(false)
                        .isDeleted(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                chatLogRepository.save(chatLog);

                em.flush();
                em.clear();

                // When: 파티 삭제
                Party foundParty = partyRepository.findById(party.getId()).orElseThrow();
                partyRepository.delete(foundParty);

                em.flush();
                em.clear();

                // Then: 파티와 채팅 로그가 모두 삭제되어야 함
                assertThat(partyRepository.findById(party.getId())).isEmpty();
                assertThat(chatLogRepository.findById(chatLog.getId())).isEmpty();
        }
}
