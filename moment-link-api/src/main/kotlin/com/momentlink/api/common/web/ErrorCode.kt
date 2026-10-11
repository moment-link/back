package com.momentlink.api.common.web

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "COMMON_001", "페이지와 조회 개수를 확인해주세요."),
    EVENT_CLOSED(HttpStatus.CONFLICT, "EVENT_005", "종료된 행사입니다."),
    EVENT_ACCESS_KEY_UNAVAILABLE(HttpStatus.CONFLICT, "EVENT_006", "기존 공유 키를 조회할 수 없습니다. 공유 키를 재발급해주세요."),
    INVALID_PARTICIPANT_SESSION(HttpStatus.UNAUTHORIZED, "PARTICIPANT_001", "유효하지 않은 참가자 세션입니다."),
    PARTICIPANT_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "PARTICIPANT_002", "참가자 세션이 만료되었습니다."),
    PARTICIPANT_SESSION_REQUIRED(HttpStatus.UNAUTHORIZED, "PARTICIPANT_003", "참가자 인증이 필요합니다."),
    PARTICIPANT_EVENT_MISMATCH(HttpStatus.FORBIDDEN, "PARTICIPANT_004", "해당 행사의 참가자 세션이 아닙니다."),
    INVALID_EVENT_ACCESS_KEY(HttpStatus.FORBIDDEN, "EVENT_003", "유효하지 않은 행사 접근 키입니다."),
    EVENT_ACCESS_DISABLED(HttpStatus.FORBIDDEN, "EVENT_004", "공개 접근이 비활성화된 행사입니다."),
    GOOGLE_USER_INFO_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "OAUTH_001", "Google 사용자 정보 응답이 올바르지 않습니다."),
    GOOGLE_TOKEN_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "OAUTH_002", "Google 토큰 응답이 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 Refresh Token입니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_002", "존재하지 않는 Refresh Token입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다."),

    INACTIVE_USER(HttpStatus.FORBIDDEN,"AUTH_003","토큰을 재발급할 수 없는 사용자입니다."),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "AUTH_004", "유효하지 않은 로그인 요청입니다."),
    INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_005", "이용할 수 없는 계정입니다."),
    NOT_FOUND_USER(HttpStatus.BAD_REQUEST, "USER_002", "사용자를 찾을 수 없습니다."),


    NOT_FOUND_EVENT(HttpStatus.BAD_REQUEST, "EVENT_002", "이벤트를 찾을 수 없습니다."),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "같은 멱등성 키에 다른 요청을 사용할 수 없습니다."),
    UPLOAD_ALREADY_COMPLETED(HttpStatus.CONFLICT, "UPLOAD_ALREADY_COMPLETED", "이미 완료된 업로드입니다."),
    UPLOAD_EXPIRED(HttpStatus.CONFLICT, "UPLOAD_EXPIRED", "이미 만료된 업로드입니다."),
}
