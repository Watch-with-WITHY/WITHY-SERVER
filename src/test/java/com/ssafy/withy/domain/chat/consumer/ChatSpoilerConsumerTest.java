package com.ssafy.withy.domain.chat.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.withy.domain.chat.dto.ChatLogDto;
import com.ssafy.withy.domain.chat.dto.ChatResponse;
import com.ssafy.withy.domain.chat.dto.SpoilerCheckResponse;
import com.ssafy.withy.domain.chat.service.SpoilerClient;
import com.ssafy.withy.domain.content.entity.Content;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.repository.PartyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChatSpoilerConsumerTest {

    private ObjectMapper objectMapper = new ObjectMapper(); // Real object
    
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private com.ssafy.withy.domain.chat.repository.ChatRepository chatRepository;
    @Mock
    private SpoilerClient spoilerClient;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private com.ssafy.withy.domain.chat.service.RedisService redisService; // 추가

    @InjectMocks
    private ChatSpoilerConsumer chatSpoilerConsumer;

    // 수동 주입 (ObjectMapper가 @InjectMocks로 잘 안 들어갈 수 있음)
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // ChatSpoilerConsumer가 생성자 주입을 사용하므로, 
        // @InjectMocks가 자동으로 ObjectMapper를 넣지만, 
        // 명시적으로 확인하거나 수동 생성하는 것이 안전함.
        chatSpoilerConsumer = new ChatSpoilerConsumer(objectMapper, partyRepository, chatRepository, spoilerClient, messagingTemplate, redisService); // RedisService 추가
        
        // ObjectMapper에 JavaTimeModule 등 등록 필요할 수 있음 (LocalDateTime 처리)
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // RedisService.getCachedPlot Mocking (기본적으로 Supplier 실행하도록 설정)
        // any()와 any(Supplier.class) 사용
        given(redisService.getCachedPlot(any(Integer.class), any())).willAnswer(invocation -> {
            java.util.function.Supplier<String> supplier = invocation.getArgument(1);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("스포일러가 없는 경우 WebSocket 전송이 발생하지 않음")
    void consume_NoSpoiler() throws Exception {
        // given
        // 1. Real ChatResponse & JSON
        ChatResponse chatResponse = ChatResponse.builder()
                .id(1)
                .userId(10)
                .partyId(100)
                .content("재미있다")
                .build();
        String jsonMsg = objectMapper.writeValueAsString(chatResponse);

        // 2. Real Entities
        Content content = Content.builder().overview("전체 줄거리").build();
        Party party = Party.builder().id(100).content(content).build(); // Assuming Builder allows setting ID or relying on Reflection if needed
        // Note: Party.id is generated via JPA usually. @Builder might not expose 'id' if generic.
        // If @Builder doesn't support ID, we might need reflection or a helper.
        // Let's assume Builder includes ID for now as implementation showed @AllArgsConstructor(access = PRIVATE) and @Builder. 
        // Usually Builder on class includes all fields.
        
        given(partyRepository.findWithContentById(100)).willReturn(party);

        // 3. Mock Spoiler Result (Safe)
        SpoilerCheckResponse.SpoilerResult safeResult = SpoilerCheckResponse.SpoilerResult.builder()
                .id("1")
                .isSpoiler(false)
                .build();
        SpoilerCheckResponse apiResponse = SpoilerCheckResponse.builder()
                .partyId("100")
                .results(Collections.singletonList(safeResult))
                .build();

        given(spoilerClient.checkSpoiler(anyString(), anyString(), anyList())).willReturn(apiResponse);

        // when
        chatSpoilerConsumer.consume(Collections.singletonList(jsonMsg));

        // then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("스포일러가 있는 경우 WebSocket으로 마스킹 메시지 전송")
    void consume_SpoilerDetected() throws Exception {
        // given
        ChatResponse chatResponse = ChatResponse.builder()
                .id(2)
                .userId(20)
                .partyId(200)
                .content("범인은 절름발이")
                .build();
        String jsonMsg = objectMapper.writeValueAsString(chatResponse);

        Content content = Content.builder().overview("반전 스릴러").build();
        Party party = Party.builder().id(200).content(content).build();

        given(partyRepository.findWithContentById(200)).willReturn(party);

        // Mock Spoiler Response (Detected)
        SpoilerCheckResponse.SpoilerResult spoilerResult = SpoilerCheckResponse.SpoilerResult.builder()
                .id("2")
                .userId("20")
                .isSpoiler(true) // DETECTED
                .reason("Major Spoiler")
                .build();
        SpoilerCheckResponse apiResponse = SpoilerCheckResponse.builder()
                .partyId("200")
                .results(Collections.singletonList(spoilerResult))
                .build();

        given(spoilerClient.checkSpoiler(anyString(), anyString(), anyList())).willReturn(apiResponse);

        // when
        chatSpoilerConsumer.consume(Collections.singletonList(jsonMsg));

        // then
        // verify proper masking message
        verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/chat/room/200"), any(ChatResponse.class));
    }
}
