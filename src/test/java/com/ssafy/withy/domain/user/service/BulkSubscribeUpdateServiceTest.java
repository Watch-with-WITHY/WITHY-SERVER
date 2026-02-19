package com.ssafy.withy.domain.user.service;

import com.ssafy.withy.domain.content.entity.Genre;
import com.ssafy.withy.domain.content.repository.GenreRepository;
import com.ssafy.withy.domain.user.dto.BulkSubscribeUpdateResponse;
import com.ssafy.withy.domain.user.entity.Subscribe;
import com.ssafy.withy.domain.user.entity.User;
import com.ssafy.withy.domain.user.repository.SubscribeRepository;
import com.ssafy.withy.domain.user.repository.UserRepository;
import com.ssafy.withy.global.error.code.GlobalErrorCode;
import com.ssafy.withy.global.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BulkSubscribeUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscribeRepository subscribeRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("구독 일괄 업데이트 - 성공 (새로운 장르 구독)")
    void updateSubscriptions_success_newSubscriptions() {
        // given
        Integer userId = 1;
        List<Integer> newGenreIds = List.of(1, 3, 5);

        User user = User.builder().id(userId).nickname("테스터").build();
        Genre genre1 = Genre.builder().id(1).name("액션").build();
        Genre genre3 = Genre.builder().id(3).name("코미디").build();
        Genre genre5 = Genre.builder().id(5).name("드라마").build();
        List<Genre> validGenres = List.of(genre1, genre3, genre5);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(genreRepository.findByIdIn(newGenreIds)).willReturn(validGenres);

        // when
        BulkSubscribeUpdateResponse response = userService.updateSubscriptions(userId, newGenreIds);

        // then
        assertThat(response.subscribedGenres()).hasSize(3);
        assertThat(response.subscribedGenres())
                .extracting("id")
                .containsExactlyInAnyOrder(1, 3, 5);

        then(subscribeRepository).should().deleteAllByUserId(userId);
        then(subscribeRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("구독 전체 해제 - 빈 배열 전송")
    void updateSubscriptions_success_emptyArray() {
        // given
        Integer userId = 1;
        List<Integer> emptyGenreIds = List.of();

        User user = User.builder().id(userId).nickname("테스터").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        BulkSubscribeUpdateResponse response = userService.updateSubscriptions(userId, emptyGenreIds);

        // then
        assertThat(response.subscribedGenres()).isEmpty();
        then(subscribeRepository).should().deleteAllByUserId(userId);
        then(subscribeRepository).should(never()).saveAll(anyList()); // 빈 배열이므로 saveAll 호출 안됨
    }

    @Test
    @DisplayName("구독 업데이트 실패 - 존재하지 않는 사용자")
    void updateSubscriptions_fail_userNotFound() {
        // given
        Integer userId = 999;
        List<Integer> genreIds = List.of(1, 3);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateSubscriptions(userId, genreIds))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("존재하지 않는 회원입니다");

        then(subscribeRepository).should(never()).deleteAllByUserId(any());
    }

    @Test
    @DisplayName("구독 업데이트 실패 - 존재하지 않는 장르 ID 포함")
    void updateSubscriptions_fail_invalidGenreId() {
        // given
        Integer userId = 1;
        List<Integer> genreIds = List.of(1, 3, 999); // 999는 존재하지 않는 장르

        User user = User.builder().id(userId).nickname("테스터").build();
        Genre genre1 = Genre.builder().id(1).name("액션").build();
        Genre genre3 = Genre.builder().id(3).name("코미디").build();
        List<Genre> validGenres = List.of(genre1, genre3); // 2개만 찾음

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(genreRepository.findByIdIn(genreIds)).willReturn(validGenres);

        // when & then
        assertThatThrownBy(() -> userService.updateSubscriptions(userId, genreIds))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("존재하지 않는 장르 ID가 포함되어 있습니다")
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("구독 업데이트 - Batch Insert 확인")
    void updateSubscriptions_batchInsert() {
        // given
        Integer userId = 1;
        List<Integer> genreIds = List.of(1, 3, 5, 7, 10); // 5개 장르

        User user = User.builder().id(userId).nickname("테스터").build();
        List<Genre> validGenres = genreIds.stream()
                .map(id -> Genre.builder().id(id).name("장르" + id).build())
                .toList();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(genreRepository.findByIdIn(genreIds)).willReturn(validGenres);

        // when
        userService.updateSubscriptions(userId, genreIds);

        // then
        ArgumentCaptor<List<Subscribe>> captor = ArgumentCaptor.forClass(List.class);
        then(subscribeRepository).should().saveAll(captor.capture());

        List<Subscribe> capturedSubscribes = captor.getValue();
        assertThat(capturedSubscribes).hasSize(5); // 5개 한 번에 저장
    }

    @Test
    @DisplayName("구독 업데이트 - 트랜잭션 원자성 (장르 검증 실패 시 삭제도 롤백)")
    void updateSubscriptions_transactionRollback() {
        // given
        Integer userId = 1;
        List<Integer> genreIds = List.of(1, 999); // 잘못된 ID 포함

        User user = User.builder().id(userId).nickname("테스터").build();
        Genre genre1 = Genre.builder().id(1).name("액션").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(genreRepository.findByIdIn(genreIds)).willReturn(List.of(genre1)); // 1개만 찾음

        // when & then
        assertThatThrownBy(() -> userService.updateSubscriptions(userId, genreIds))
                .isInstanceOf(CustomException.class);

        // 장르 검증이 delete보다 먼저 일어나므로, 검증 실패 시 delete는 호출되지 않음
        then(subscribeRepository).should(never()).deleteAllByUserId(userId);
        then(subscribeRepository).should(never()).saveAll(anyList());
    }
}
