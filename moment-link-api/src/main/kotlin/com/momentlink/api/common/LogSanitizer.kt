package com.momentlink.api.common

/**
 * 요청에서 들어온 값을 로그에 남길 때, 줄바꿈으로 가짜 로그 줄을 만들지 못하게 정리한다.
 */
object LogSanitizer {
    const val MAX_LENGTH = 100

    private val CONTROL_CHARACTERS = Regex("\\p{Cntrl}")

    fun sanitize(value: String?): String =
        value?.replace(CONTROL_CHARACTERS, "_")?.take(MAX_LENGTH) ?: "-"
}
