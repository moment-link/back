package com.momentlink.api.transfer.worker

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "transfer.worker")
data class TransferWorkerProperties(
    val enabled: Boolean,
    val pollIntervalMs: Long,
    val batchSize: Int,
    val maxAttempts: Int,
    val staleAfter: Duration,
)
