package com.momentlink.domain.storage

enum class StorageProvider(
    val pathValue: String,
) {
    GOOGLE_DRIVE("google-drive"),
    DROPBOX("dropbox"),
    ONE_DRIVE("one-drive"), ;

    companion object {
        fun resolve(provider: String): StorageProvider?
            = entries.firstOrNull {
                it.pathValue.equals(provider, ignoreCase = true)
            }
    }
}