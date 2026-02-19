package com.ssafy.withy.global.error.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "잘못된 입력값입니다."),
    INVALID_REQUEST(400, "잘못된 요청입니다."),
    METHOD_NOT_ALLOWED(405, "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
    ENTITY_NOT_FOUND(404, "대상을 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(404, "해당 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(403, "접근 권한이 없습니다."),
    FILE_NOT_FOUND(404, "파일을 찾을 수 없습니다."),

    CONFLICT(409, "이미 존재하는 리소스입니다."),
    ALREADY_REPORTED(409, "이미 신고한 채팅입니다."),

    // User
    USER_DUPLICATE_EMAIL(409, "이미 존재하는 이메일입니다."),
    USER_DUPLICATE_NICKNAME(409, "이미 존재하는 닉네임입니다."),
    USER_NOT_FOUND(404, "존재하지 않는 회원입니다."),
    USER_SIGNUP_ERROR(409, "회원가입 중 알 수 없는 오류가 발생했습니다.."),
    ALREADY_EXIST_EMAIL(409, "이미 존재하는 이메일입니다."),
    ALREADY_EXIST_NICKNAME(409, "이미 존재하는 닉네임입니다."),
    SOCIAL_USER_CANNOT_CHANGE_PASSWORD(400, "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),
    PASSWORD_NOT_MATCH(400, "비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD(400, "기존과 동일한 비밀번호는 사용할 수 없습니다."),

    // FriendRequest
    NOT_REQUESTER(400,  "본인의 요청만 취소할 수 있습니다."),
    CANNOT_CANCEL_PROCESSED_REQUEST(403, "이미 처리된 친구 요청입니다."),
    FRIEND_REQUEST_NOT_FOUND(404, "친구 요청을 찾을 수 없습니다."),

    // Auth
    LOGIN_FAILED(401, "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(404, "리프레시 토큰이 존재하지 않습니다."),
    INVALID_REFRESH_TOKEN(401, "리프레시 토큰이 유효하지 않거나 일치하지 않습니다."),

    // Party
    PLATFORM_TYPE_NOT_FOUND(404, "잘못된 플랫폼 타입입니다."),
    PARTY_NOT_FOUND(404, "파티를 찾을 수 없습니다."),
    PARTY_FULL(400, "파티 인원이 가득 찼습니다."),
    PARTY_USER_NOT_JOINED(400, "해당 유저는 파티 참여자가 아닙니다."),
    PARTY_NOT_PARTY_HOST(403, "방장만 가능한 작업입니다."),
    PARTY_ALREADY_ACTIVE(400, "이미 시작된 파티입니다."),
    PARTY_INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다."),
    PARTY_ALREADY_BANNED_USER(403, "강퇴당하여 재입장이 불가능합니다."),
    PARTY_ALREADY_JOINED_USER(403, "이미 참가된 파티입니다."),
    PARTY_CANNOT_CHANGE_PLATFORM(403, "파티의 플랫폼은 변경할 수 없습니다."),
    PARTY_HOST_CANNOT_EXIT(403, "방장은 퇴장이 불가능합니다."),
    INVALID_PARTY_TIME(400, "파티 시작 시간은 현재 시간 이후여야 합니다."),

    // Participant
    PARTICIPANT_NOT_FOUND(404, "참여자를 찾을 수 없습니다."),
    NOT_ENOUGH_AUTHORITY(403, "권한이 부족합니다."),
    PARTY_HOST_AUTH_REQUIRED(401, "HOST가 아닙니다."),

    // Content
    CONTENT_NOT_FOUND(404, "존재하지 않는 컨텐츠입니다."),

    // DM & Chat
    CHAT_NOT_FOUND(404, "존재하지 않는 채팅 메시지입니다."),
    CHAT_DELETE_FORBIDDEN(403, "채팅 메시지를 삭제할 권한이 없습니다."),

    // DM
    DM_ROOM_NOT_FOUND(404, "존재하지 않는 DM 방입니다."),
    DM_ROOM_ACCESS_DENIED(403, "해당 DM 방에 접근 권한이 없습니다."),
    DM_SELF_NOT_ALLOWED(400, "자기 자신과는 DM을 생성할 수 없습니다."),

    // Genre
    GENRE_NOT_FOUND(404, "존재하지 않는 장르입니다."),

    // API
    TMDB_API_ERROR(500, "TMDB API 요청 중 오류가 발생했습니다."),
    YOUTUBE_API_ERROR(500, "YouTube API 요청 중 오류가 발생했습니다."),
    TRANSLATION_API_ERROR(500, "번역 API 요청 중 오류가 발생했습니다."),

    // S3
    S3_UPLOAD_FAILED(500, "파일 업로드에 실패했습니다."),
    S3_DELETE_FAILED(500, "파일 삭제에 실패했습니다."),

    // Email
    EMAIL_SEND_FAILED(500, "이메일 전송에 실패했습니다."),
    EMAIL_NOT_VERIFIED(400, "이메일 인증이 완료되지 않았습니다."),
    INVALID_AUTH_CODE(400, "유효하지 않은 인증 코드입니다."),

    // Missing Codes
    UNAUTHORIZED_USER(401, "인증되지 않은 사용자입니다.");

    private final int status;
    private final String message;
}
