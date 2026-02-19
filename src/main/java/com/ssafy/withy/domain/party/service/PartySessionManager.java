package com.ssafy.withy.domain.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class PartySessionManager {

    // SessionId -> PartyId
    private final Map<String, Integer> sessionPartyMap = new ConcurrentHashMap<>();
    // SessionId -> UserId
    private final Map<String, Integer> sessionUserMap = new ConcurrentHashMap<>();

    // PartyId -> UserId -> Set<SessionId> (Effective Multiset for online status)
    private final Map<Integer, Map<Integer, Set<String>>> partyUserSessions = new ConcurrentHashMap<>();

    // PartyId -> ScheduledFuture (Host cleanup task)
    private final Map<Integer, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final PartyService partyService;

    // 생성자 주입 (Circular Dependency 방지를 위해 @Lazy 사용)
    public PartySessionManager(TaskScheduler taskScheduler, @Lazy PartyService partyService) {
        this.taskScheduler = taskScheduler;
        this.partyService = partyService;
    }

    public void scheduleHostCleanup(Integer partyId) {
        if (cleanupTasks.containsKey(partyId)) {
            return; // 이미 스케줄링됨
        }
        
        log.info("[SessionManager] Hosting cleanup scheduled for party {}", partyId);
        
        // 30초 후 삭제 -> 테스트를 위해 1시간(3600초)으로 변경
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    log.info("[SessionManager] Executing cleanup for party {}", partyId);
                    cleanupTasks.remove(partyId);
                    try {
                        partyService.deletePartySystem(partyId);
                    } catch (Exception e) {
                        log.error("Error during host cleanup", e);
                    }
                },
                Instant.now().plusSeconds(3600*100) // TODO: 배포 시 30초로 원복 필요
        );
        
        cleanupTasks.put(partyId, future);
    }

    public void cancelHostCleanup(Integer partyId) {
        ScheduledFuture<?> future = cleanupTasks.remove(partyId);
        if (future != null) {
            future.cancel(false);
            log.info("[SessionManager] Host cleanup cancelled for party {}", partyId);
        }
    }

    public void registerSession(String sessionId, Integer partyId, Integer userId) {
        log.info("[SessionManager] Registering session: {} -> Party {}, User {}", sessionId, partyId, userId);
        sessionPartyMap.put(sessionId, partyId);
        sessionUserMap.put(sessionId, userId);

        partyUserSessions.computeIfAbsent(partyId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public Integer getPartyId(String sessionId) {
        return sessionPartyMap.get(sessionId);
    }

    public Integer getUserId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public void removeSession(String sessionId) {
        Integer partyId = sessionPartyMap.remove(sessionId);
        Integer userId = sessionUserMap.remove(sessionId);

        log.info("[SessionManager] Removing session: {}, Found Party: {}, User: {}", sessionId, partyId, userId);

        if (partyId != null && userId != null) {
            Map<Integer, Set<String>> userSessions = partyUserSessions.get(partyId);
            if (userSessions != null) {
                Set<String> sessions = userSessions.get(userId);
                if (sessions != null) {
                    boolean removed = sessions.remove(sessionId);
                    log.info("[SessionManager] Removed sessionId from set: {}", removed);
                    if (sessions.isEmpty()) {
                        userSessions.remove(userId);
                        log.info("[SessionManager] User {} has no more sessions. Removed from party.", userId);
                    }
                }
                if (userSessions.isEmpty()) {
                    partyUserSessions.remove(partyId);
                }
            }
        }
    }

    public boolean isUserOnline(Integer partyId, Integer userId) {
        Map<Integer, Set<String>> userSessions = partyUserSessions.get(partyId);
        boolean online = userSessions != null && userSessions.containsKey(userId);
        log.info("[SessionManager] Checking online status for Party {}, User {}: {}", partyId, userId, online);
        return online;
    }
}
