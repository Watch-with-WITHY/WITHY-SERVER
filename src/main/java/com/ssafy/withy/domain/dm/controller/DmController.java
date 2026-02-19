package com.ssafy.withy.domain.dm.controller;

import com.ssafy.withy.domain.dm.dto.DmMessageResponse;
import com.ssafy.withy.domain.dm.dto.DmRoomRequest;
import com.ssafy.withy.domain.dm.dto.DmRoomResponse;
import com.ssafy.withy.domain.dm.service.DmService;
import com.ssafy.withy.global.auth.dto.CustomUserDetails;
import com.ssafy.withy.global.common.code.GlobalSuccessCode;
import com.ssafy.withy.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm")
@RequiredArgsConstructor
@Tag(name = "DM", description = "다이렉트 메시지 API")
public class DmController {
    
    private final DmService dmService;
    
    /**
     * DM 방 생성
     */
    @Operation(
            summary = "DM 방 생성",
            description = """
                    - 상대방과의 DM 방을 생성합니다
                    - 최초 메시지 전송 시점에 호출합니다
                    - 이미 방이 존재하면 기존 방 정보를 반환합니다 (Idempotent)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "DM 방 생성 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<DmRoomResponse>> createRoom(
            @Valid @RequestBody DmRoomRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Integer currentUserId = userDetails.getUserId();
        
        DmRoomResponse response = dmService.createRoom(currentUserId, request.getTargetUserId());
        
        return ResponseEntity.ok(
                ApiResponse.success(GlobalSuccessCode.CREATE_DM_ROOM_SUCCESS, response)
        );
    }

    /**
     * 상대방 ID로 DM 방 존재 여부 조회
     */
    @Operation(
            summary = "상대방 ID로 DM 방 조회",
            description = """
                    - 상대방과의 DM 방이 이미 존재하는지 확인합니다
                    - 방이 존재하지 않거나, 나간 상태라면 404를 반환합니다
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "DM 방 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "DM 방이 존재하지 않음",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/rooms/target/{targetUserId}")
    public ResponseEntity<ApiResponse<DmRoomResponse>> getRoomByTargetUser(
            @Parameter(description = "상대방 사용자 ID", required = true, example = "2")
            @PathVariable Integer targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Integer currentUserId = userDetails.getUserId();

        DmRoomResponse response = dmService.getRoomByTargetIds(currentUserId, targetUserId);

        return ResponseEntity.ok(
                ApiResponse.success(GlobalSuccessCode.GET_DM_ROOMS_SUCCESS, response)
        );
    }
    
    /**
     * 내 DM 방 목록 조회
     */
    @Operation(
            summary = "내 DM 방 목록 조회",
            description = """
                    - 현재 로그인한 사용자가 참여 중인 모든 DM 방 목록을 조회합니다
                    - 최근 메시지가 있는 방부터 정렬됩니다
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "DM 방 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<DmRoomResponse>>> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Integer currentUserId = userDetails.getUserId();
        
        List<DmRoomResponse> response = dmService.getMyRooms(currentUserId);
        
        return ResponseEntity.ok(
                ApiResponse.success(GlobalSuccessCode.GET_DM_ROOMS_SUCCESS, response)
        );
    }
    
    /**
     * DM 메시지 목록 조회
     */
    @Operation(
            summary = "DM 메시지 목록 조회",
            description = """
                    - 특정 DM 방의 메시지 목록을 조회합니다
                    - 페이지네이션을 지원하여 한 번에 20개씩 조회합니다
                    - 최신 메시지부터 내림차순으로 정렬됩니다
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "DM 메시지 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 DM 방에 접근 권한이 없음",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 DM 방",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<DmMessageResponse>>> getMessages(
            @Parameter(description = "DM 방 ID", required = true, example = "1")
            @PathVariable Integer roomId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Integer currentUserId = userDetails.getUserId();
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DmMessageResponse> response = dmService.getMessages(roomId, currentUserId, pageable);
        
        return ResponseEntity.ok(
                ApiResponse.success(GlobalSuccessCode.GET_DM_MESSAGES_SUCCESS, response)
        );
    }
    
    /**
     * DM 방 나가기 (삭제)
     */
    @Operation(
            summary = "DM 방 나가기 (삭제)",
            description = """
                    - 사용자가 특정 DM 방을 나갑니다 (목록에서 삭제)
                    - 상대방에게는 방이 유지됩니다
                    - 추후 메시지를 보내거나 받으면 다시 방이 생성(복구)됩니다
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "DM 방 나가기 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 DM 방에 권한이 없음",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 DM 방",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @Parameter(description = "DM 방 ID", required = true, example = "1")
            @PathVariable Integer roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Integer currentUserId = userDetails.getUserId();
        dmService.deleteRoom(roomId, currentUserId);
        
        return ResponseEntity.ok(
                ApiResponse.success(GlobalSuccessCode.DELETE_DM_ROOM_SUCCESS, null)
        );
    }
}
