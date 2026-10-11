package com.momentlink.api.common.web

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommonResponse<T>(
    val success: Boolean,
    val code: String?, // 비즈니스 에러일 때만 값 존재
    val message: String,
    val timestamp: Instant,
    val data: T?,
) {

    companion object {
        const val SUCCESS: String = "요청이 성공적으로 처리되었습니다."

        fun <T> success(data: T): CommonResponse<T>
        = CommonResponse(true, null, SUCCESS, Instant.now(), data)

        fun success(): CommonResponse<Unit>
        = CommonResponse(true, null, SUCCESS, Instant.now(), null)

        fun error(errorCode: ErrorCode): CommonResponse<Unit>
        = CommonResponse(false, errorCode.code, errorCode.message, Instant.now(), null)
    }
}
