package com.momentlink.api.media.infra.s3

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration

@Configuration
class S3Config(
    private val properties: S3Properties,
) {
    // 자격 증명은 기본 체인(로컬 AWS profile, EC2 IAM Role)을 쓴다.
    @Bean(destroyMethod = "close")
    fun s3Client(): S3Client =
        S3Client.builder()
            .region(Region.of(properties.region))
            .overrideConfiguration {
                it.apiCallTimeout(API_CALL_TIMEOUT).apiCallAttemptTimeout(API_ATTEMPT_TIMEOUT)
            }
            .build()

    @Bean(destroyMethod = "close")
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .region(Region.of(properties.region))
            .build()

    companion object {
        private val API_CALL_TIMEOUT = Duration.ofSeconds(10)
        private val API_ATTEMPT_TIMEOUT = Duration.ofSeconds(5)
    }
}
