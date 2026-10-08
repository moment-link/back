package com.momentlink.domain.media

enum class TransferStatus {
    NOT_STARTED,
    PENDING,
    IN_PROGRESS,
    RETRY_WAIT,
    SUCCEEDED,
    PERMANENT_FAILED,
}

enum class TransferAttemptResult {
    SUCCEEDED,
    RETRYABLE_FAILED,
    PERMANENT_FAILED,
}

enum class TransferFailureType {
    RATE_LIMITED,
    TIMEOUT,
    NETWORK,
    PROVIDER_SERVER_ERROR,
    AUTHENTICATION,
    PERMISSION,
    INVALID_DESTINATION,
    UNKNOWN,
}