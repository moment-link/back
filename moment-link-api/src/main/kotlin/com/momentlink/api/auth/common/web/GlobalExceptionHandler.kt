package com.momentlink.api.auth.common.web

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.error
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<CommonResponse<Unit>> {
        return ResponseEntity
            .status(ex.errorCode.httpStatus)
            .body(error(ex.errorCode))
    }
}