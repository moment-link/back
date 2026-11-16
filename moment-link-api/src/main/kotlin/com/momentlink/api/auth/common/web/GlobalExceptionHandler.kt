package com.momentlink.api.auth.common.web

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.LogSanitizer
import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.error
import com.momentlink.api.common.web.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException, request: HttpServletRequest): ResponseEntity<CommonResponse<Unit>> {
        val errorCode = ex.errorCode
        val detail = ex.reason ?: errorCode.message
        if (errorCode.httpStatus.is5xxServerError) {
            log.error("[{}] {} {} - {}", errorCode.code, request.method, request.requestURI, detail, ex)
        } else {
            log.warn("[{}] {} {} - {}", errorCode.code, request.method, request.requestURI, detail)
        }

        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(error(errorCode))
    }

    // 처리되지 않은 예외는 원인을 남기고 공통 응답 형식의 500으로 돌려준다.
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception, request: HttpServletRequest): ResponseEntity<CommonResponse<Unit>> {
        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR
        log.error("[{}] {} {}", errorCode.code, request.method, request.requestURI, ex)

        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(error(errorCode))
    }

    // 쿠키 누락, 잘못된 파라미터처럼 Spring이 처리하는 요청 오류는 원래 상태 코드를 유지한 채 남긴다.
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val target = describe(request)
        if (statusCode.is5xxServerError) {
            log.error("[{}] {}", statusCode.value(), target, ex)
        } else {
            log.warn("[{}] {} - {}", statusCode.value(), target, LogSanitizer.sanitize(ex.message))
        }

        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    private fun describe(request: WebRequest): String {
        val servletRequest = (request as? ServletWebRequest)?.request
            ?: return request.getDescription(false)
        return "${servletRequest.method} ${servletRequest.requestURI}"
    }
}
