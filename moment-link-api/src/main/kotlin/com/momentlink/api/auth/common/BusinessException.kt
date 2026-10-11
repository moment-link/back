package com.momentlink.api.auth.common

import com.momentlink.api.common.web.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
    case: Throwable? = null,
): RuntimeException(errorCode.message)