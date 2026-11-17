package com.momentlink.api.common.web

import org.springframework.data.domain.Slice

data class SliceResDto<T>(
    val items: List<T>,
    val nextPage: Int?,
    val hasNext: Boolean,
) {
    companion object {
        fun <E : Any, T> from(slice: Slice<E>, mapper: (E) -> T): SliceResDto<T> =
            SliceResDto(
                items = slice.content.map(mapper),
                nextPage = if (slice.hasNext()) slice.number + 1 else null,
                hasNext = slice.hasNext(),
            )

        fun <T> empty(): SliceResDto<T> = SliceResDto(emptyList(), null, false)
    }
}
