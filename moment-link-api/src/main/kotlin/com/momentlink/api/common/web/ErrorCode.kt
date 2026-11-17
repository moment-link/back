package com.momentlink.api.common.web

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "COMMON_001", "페이지와 조회 개수를 확인해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 오류가 발생했습니다."),
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
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "OAUTH_003", "소셜 로그인이 취소되었거나 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 Refresh Token입니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_002", "존재하지 않는 Refresh Token입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 사용자입니다."),

    INACTIVE_USER(HttpStatus.FORBIDDEN,"AUTH_003","토큰을 재발급할 수 없는 사용자입니다."),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "AUTH_004", "유효하지 않은 로그인 요청입니다."),
    INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_005", "이용할 수 없는 계정입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_006", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_007", "접근 권한이 없습니다."),
    NOT_FOUND_USER(HttpStatus.BAD_REQUEST, "USER_002", "사용자를 찾을 수 없습니다."),


    NOT_FOUND_EVENT(HttpStatus.NOT_FOUND, "EVENT_002", "이벤트를 찾을 수 없습니다."),
    EVENT_PERIOD_CONFLICTS_MOMENT(HttpStatus.CONFLICT, "EVENT_007", "변경한 행사 기간을 벗어나는 모먼트가 있습니다."),

    INVALID_DISPLAY_NAME(HttpStatus.BAD_REQUEST, "PARTICIPANT_005", "닉네임은 앞뒤 공백을 제외하고 2~20자여야 합니다."),
    DISPLAY_NAME_CONFLICT(HttpStatus.CONFLICT, "PARTICIPANT_006", "이미 사용 중인 닉네임입니다."),

    MOMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "MOMENT_001", "모먼트를 찾을 수 없습니다."),
    INVALID_MOMENT(HttpStatus.BAD_REQUEST, "MOMENT_002", "모먼트 입력값을 확인해주세요."),
    MOMENT_OUT_OF_EVENT_PERIOD(HttpStatus.BAD_REQUEST, "MOMENT_003", "모먼트는 행사 기간 안에 있어야 합니다."),

    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "UPLOAD_001", "Idempotency-Key를 확인해주세요."),
    INVALID_MEDIA_METADATA(HttpStatus.BAD_REQUEST, "UPLOAD_002", "파일 정보를 확인해주세요."),
    MEDIA_SIZE_LIMIT_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_003", "업로드할 수 있는 파일 크기를 초과했습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UPLOAD_004", "지원하지 않는 파일 형식입니다."),
    EVENT_UPLOAD_CLOSED(HttpStatus.CONFLICT, "UPLOAD_005", "업로드가 마감된 행사입니다."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "UPLOAD_006", "같은 Idempotency-Key로 다른 요청을 보낼 수 없습니다."),
    UPLOAD_EXPIRED(HttpStatus.CONFLICT, "UPLOAD_007", "업로드 기한이 지났습니다. 새로 준비해주세요."),
    UPLOAD_OBJECT_NOT_FOUND(HttpStatus.CONFLICT, "UPLOAD_008", "업로드된 파일을 찾을 수 없습니다."),
    UPLOAD_VERIFICATION_FAILED(HttpStatus.CONFLICT, "UPLOAD_009", "업로드된 파일의 크기 또는 체크섬이 일치하지 않습니다."),
    INVALID_UPLOAD_STATE(HttpStatus.CONFLICT, "UPLOAD_010", "현재 업로드 상태에서는 처리할 수 없습니다."),
    MEDIA_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_011", "파일 저장소에 일시적으로 접근할 수 없습니다."),

    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDIA_001", "미디어를 찾을 수 없습니다."),

    UNSUPPORTED_STORAGE_PROVIDER(HttpStatus.BAD_REQUEST, "STORAGE_001", "지원하지 않는 저장소입니다."),
    INVALID_STORAGE_STATE(HttpStatus.BAD_REQUEST, "STORAGE_002", "유효하지 않은 저장소 연결 요청입니다."),
    STORAGE_CONNECT_FAILED(HttpStatus.BAD_GATEWAY, "STORAGE_003", "저장소 연결에 실패했습니다."),
    STORAGE_NOT_CONNECTED(HttpStatus.CONFLICT, "STORAGE_004", "저장소가 연결되어 있지 않거나 재인증이 필요합니다."),

    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSFER_001", "전송 작업을 찾을 수 없습니다."),
    TRANSFER_NOT_RETRYABLE(HttpStatus.CONFLICT, "TRANSFER_002", "진행 중이거나 완료된 전송은 재시도할 수 없습니다."),
    TRANSFER_MEDIA_NOT_ELIGIBLE(HttpStatus.CONFLICT, "TRANSFER_003", "삭제되었거나 공개되지 않은 미디어입니다."),
}
