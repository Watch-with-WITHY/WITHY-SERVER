package com.ssafy.withy.global.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalSuccessCode implements SuccessCode {

    // User
    USER_SIGNUP_SUCCESS(201, "회원가입에 성공하였습니다."),
    GET_MY_PROFILE_SUCCESS(200, "GET - 상세 정보 불러오기를 성공했습니다."),
    USER_UPDATE_SUCCESS(200, "사용자 정보 수정에 성공했습니다."),
    USER_WITHDRAWAL_SUCCESS(200, "회원 탈퇴에 성공했습니다."),
    GET_MY_SUBSCRIBE_LIST_SUCCESS(200, "GET - 구독 리스트 조회를 성공했습니다."),
    UNSUBSCRIBE_SUCCESS(200, "구독 해제에 성공했습니다."),
    BULK_UPDATE_SUBSCRIPTION_SUCCESS(200, "구독 상태가 업데이트되었습니다."),
    USER_PASSWORD_CHANGE_SUCCESS(200, "비밀번호 변경에 성공했습니다."),
    USER_LANGUAGE_UPDATE_SUCCESS(200, "선호 언어 설정이 완료되었습니다."),
    USER_ONBOARDING_COMPLETE(200, "유저 온보딩이 완료되었습니다."),

    // Chat
    DELETE_CHAT_SUCCESS(200, "채팅 삭제에 성공했습니다."),
    TRANSLATE_CHAT_SUCCESS(200, "번역에 성공했습니다."),

    // DM
    CREATE_DM_ROOM_SUCCESS(200, "DM 방 생성/조회에 성공했습니다."),
    GET_DM_ROOMS_SUCCESS(200, "GET - DM 방 목록 조회를 성공했습니다."),
    GET_DM_MESSAGES_SUCCESS(200, "GET - DM 메시지 목록 조회를 성공했습니다."),
    DELETE_DM_ROOM_SUCCESS(200, "DM 방 나가기(삭제)에 성공했습니다."),
    CREATE_DM_MESSAGE_SUCCESS(200, "DM 메시지 전송에 성공했습니다."),

    // Auth
    LOGIN_SUCCESS(200, "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(200, "로그아웃에 성공했습니다."),
    REISSUE_SUCCESS(200, "토큰 재발급에 성공했습니다."),

    // Email
    EMAIL_SEND_SUCCESS(200, "이메일 전송에 성공했습니다."),
    EMAIL_VERIFY_SUCCESS(200, "이메일 인증에 성공했습니다."),

    // Party
    GET_PARTY_LIST_SUCCESS(200, "GET - 파티 목록 조회를 성공했습니다."),
    GET_PLATFORM_TYPES_SUCCESS(200, "GET - 플랫폼 타입 리스트 불러오기를 성공했습니다."),
    PARTY_CREATE_SUCCESS(201, "파티 생성에 성공했습니다."),
    GET_PARTY_DETAIL_SUCCESS(200, "파티 상세 조회가 완료되었습니다."),
    UPDATE_PARTY_SUCCESS(200, "파티 정보 수정이 완료되었습니다."),
    ACTIVATE_PARTY_SUCCESS(200, "파티가 활성화되었습니다."),
    PARTY_ENTER_CHECK_SUCCESS(200, "파티 입장 가능 여부 조회에 성공하였습니다.."),
    DELETE_PARTY_SUCCESS(200, "파티가 종료(삭제)되었습니다."),
    GET_PARTICIPANTS_SUCCESS(200, "참여자 목록 조회가 완료되었습니다."),
    BAN_PARTICIPANT_SUCCESS(200, "참여자를 내보냈습니다."), // 강퇴/차단
    UPDATE_ROLE_SUCCESS(200, "참여자 권한이 변경되었습니다."),
    PARTY_JOIN_SUCCESS(200, "파티에 입장하였습니다."),
    PARTY_EXIT_SUCCESS(200, "파티에 퇴장하였습니다."),

    // Content
    GET_GENRE_LIST_SUCCESS(200, "GET - 장르 목록 조회를 성공했습니다."),

    // User Onboarding
    CHECK_EMAIL_SUCCESS(200, "이메일 중복 조회에 성공하였습니다."),
    CHECK_NICKNAME_SUCCESS(200, "닉네임 중복 조회에 성공하였습니다."),
    GENERATE_NICKNAME_SUCCESS(200, "랜덤 닉네임 생성이 완료되었습니다."),
    UPDATE_NICKNAME_SUCCESS(200, "닉네임 변경이 완료되었습니다."),
    UPDATE_PREFERENCES_SUCCESS(200, "선호 장르 설정이 완료되었습니다."),
    GET_MY_CHAT_LOGS_SUCCESS(200, "채팅 로그 조회에 성공했습니다.");

    private final int status;
    private final String message;
}
