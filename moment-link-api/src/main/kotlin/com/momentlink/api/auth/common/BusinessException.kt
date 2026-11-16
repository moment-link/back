package com.momentlink.api.auth.common

import com.momentlink.api.common.web.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
    case: Throwable? = null,
    // 응답에는 나가지 않고 GlobalExceptionHandler 로그에만 남는 상세 사유
    val reason: String? = null,
): RuntimeException(errorCode.message)