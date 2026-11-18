package com.momentlink.api.moment.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.momentlink.domain.moment.Moment
import java.time.Instant

data class MomentCreateReqDto(
    val title: String,
    val place: String = "",
    val startsAt: Instant,
    val endsAt: Instant,
    val sortOrder: Int = 0,
)

// 생략한 필드는 유지하고, 명시적 null은 Jackson 단계에서 400으로 막는다.
data class MomentUpdateReqDto(
    @JsonSetter(nulls = Nulls.FAIL) val title: String? = null,
    @JsonSetter(nulls = Nulls.FAIL) val place: String? = null,
    @JsonSetter(nulls = Nulls.FAIL) val startsAt: Instant? = null,
    @JsonSetter(nulls = Nulls.FAIL) val endsAt: Instant? = null,
    @JsonSetter(nulls = Nulls.FAIL) val sortOrder: Int? = null,
)

data class MomentResDto(
    val momentId: Long,
    val eventId: Long,
    val title: String,
    val place: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val sortOrder: Int,
) {
    companion object {
        fun from(moment: Moment): MomentResDto =
            MomentResDto(
                momentId = moment.requiredId(),
                eventId = moment.eventId,
                title = moment.title,
                place = moment.place,
                startsAt = moment.startsAt,
                endsAt = moment.endsAt,
                sortOrder = moment.sortOrder,
            )
    }
}

data class MomentListItemResDto(
    val momentId: Long,
    val title: String,
    val place: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val sortOrder: Int,
) {
    companion object {
        fun from(moment: Moment): MomentListItemResDto =
            MomentListItemResDto(
                momentId = moment.requiredId(),
                title = moment.title,
                place = moment.place,
                startsAt = moment.startsAt,
                endsAt = moment.endsAt,
                sortOrder = moment.sortOrder,
            )
    }
}

data class MomentListResDto(
    val items: List<MomentListItemResDto>,
) {
    companion object {
        fun from(moments: List<Moment>): MomentListResDto =
            MomentListResDto(moments.map(MomentListItemResDto::from))
    }
}

data class MomentDeleteResDto(
    val momentId: Long,
    val deleted: Boolean,
)
