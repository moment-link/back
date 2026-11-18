package com.momentlink.api.storage.dto

import com.momentlink.domain.storage.Storage
import com.momentlink.domain.storage.StorageProvider
import com.momentlink.domain.storage.StorageStatus
import java.time.Instant

data class StorageConnectReqDto(
    val provider: StorageProvider,
)

data class StorageConnectResDto(
    val authorizationUrl: String,
    val expiresAt: Instant,
)

// 컨트롤러가 state 쿠키를 붙이는 데 쓰고 응답 본문에는 넣지 않는다.
data class StorageConnectResult(
    val authorizationUrl: String,
    val expiresAt: Instant,
    val state: String,
)

data class StorageStatusResDto(
    val storage: StorageResDto?,
)

data class StorageResDto(
    val storageId: Long,
    val provider: StorageProvider,
    val accountEmail: String,
    val status: StorageStatus,
) {
    companion object {
        fun from(storage: Storage): StorageResDto =
            StorageResDto(storage.requiredId(), storage.provider, storage.accountEmail, storage.status)
    }
}

data class StorageDisconnectResDto(
    val status: StorageStatus,
)
