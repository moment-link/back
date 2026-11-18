package com.momentlink.api.storage.infra.google

import com.momentlink.domain.media.TransferFailureType

// 외부 응답 원문은 담지 않는다. 메시지는 우리가 만든 문장만 넣는다.
class DriveTransferException(
    val failureType: TransferFailureType,
    val retryable: Boolean,
    message: String,
) : RuntimeException(message)
