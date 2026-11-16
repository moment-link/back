package com.momentlink.api.common.web

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

/**
 * GlobalExceptionHandler까지 오지 않는 필터·시큐리티 단계 오류를 같은 CommonResponse 형식으로 응답한다.
 */
@Component
class ErrorResponseWriter(
    private val jsonMapper: JsonMapper,
) {
    fun write(response: HttpServletResponse, errorCode: ErrorCode) {
        response.status = errorCode.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, CommonResponse.error(errorCode))
    }
}
