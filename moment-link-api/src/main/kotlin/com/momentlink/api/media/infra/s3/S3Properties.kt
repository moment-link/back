package com.momentlink.api.media.infra.s3

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "media.s3")
data class S3Properties(
    val bucket: String,
    val region: String,
)
