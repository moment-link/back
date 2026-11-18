package com.momentlink.api.media.application

import org.springframework.data.domain.Sort

// 내 업로드·주최자 목록: 준비 레코드 생성 시각 기준, 동률은 ID
enum class MediaSort {
    LATEST,
    OLDEST;

    fun toSort(): Sort =
        when (this) {
            LATEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
            OLDEST -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        }
}

// 갤러리: 업로드 완료 시각 또는 좋아요 수 기준, 동률은 ID
enum class GallerySort {
    LATEST,
    OLDEST,
    POPULAR;

    fun toSort(): Sort =
        when (this) {
            LATEST -> Sort.by(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id"))
            OLDEST -> Sort.by(Sort.Order.asc("uploadedAt"), Sort.Order.asc("id"))
            POPULAR -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("id"))
        }
}
