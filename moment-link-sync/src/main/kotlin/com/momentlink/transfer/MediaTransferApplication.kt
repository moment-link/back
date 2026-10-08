package com.momentlink.transfer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.momentlink"])
class MediaTransferApplication

fun main(args: Array<String>) {
    runApplication<MediaTransferApplication>(*args)
}
