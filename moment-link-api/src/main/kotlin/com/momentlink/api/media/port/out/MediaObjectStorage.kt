package com.momentlink.api.media.port.out

import java.io.InputStream
import java.time.Duration
import java.time.Instant

interface MediaObjectStorage {
    fun presignUpload(objectKey: String, mimeType: String, checksum: String, validity: Duration): UploadTarget
    fun presignDownload(objectKey: String, validity: Duration): DownloadTarget
    fun head(objectKey: String): StoredObjectInfo
    fun open(objectKey: String): InputStream
    fun delete(objectKey: String)
}

data class UploadTarget(
    val url: String,
    val headers: Map<String, String>,
)

data class DownloadTarget(
    val url: String,
    val expiresAt: Instant,
)

data class StoredObjectInfo(
    val sizeBytes: Long,
    val checksum: String?,
    val eTag: String,
)
