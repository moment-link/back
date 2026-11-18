package com.momentlink.api.moment.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.domain.event.Event
import java.time.Instant

data class MomentDraft(
    val title: String,
    val place: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val sortOrder: Int,
)

object MomentPolicy {
    private const val MAX_TEXT_LENGTH = 200

    fun validate(draft: MomentDraft, event: Event) {
        val isInvalid = draft.title.isEmpty() ||
            draft.title.length > MAX_TEXT_LENGTH ||
            draft.place.length > MAX_TEXT_LENGTH ||
            draft.sortOrder < 0 ||
            !draft.endsAt.isAfter(draft.startsAt)
        if (isInvalid) throw BusinessException(ErrorCode.INVALID_MOMENT)

        if (draft.startsAt.isBefore(event.startsAt) || draft.endsAt.isAfter(event.endsAt)) {
            throw BusinessException(ErrorCode.MOMENT_OUT_OF_EVENT_PERIOD)
        }
    }
}
