package com.momentlink.api.storage.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    val frontendRedirectUrl: String,
    val tokenEncryptionKey: String,
    val googleDrive: GoogleDrive,
) {
    data class GoogleDrive(
        val clientId: String,
        val clientSecret: String,
        val authorizationUri: String,
        val tokenUri: String,
        val userInfoUri: String,
        val redirectUri: String,
        val folderPrefix: String,
    )
}
