package com.momentlink.api.transfer.port.out

import com.momentlink.domain.media.MediaTransferAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MediaTransferAttemptRepository : JpaRepository<MediaTransferAttempt, Long> {

    fun findAllByMediaTransferIdOrderByAttemptNumberDesc(mediaTransferId: Long): List<MediaTransferAttempt>

    fun findByMediaTransferIdAndAttemptNumber(mediaTransferId: Long, attemptNumber: Int): MediaTransferAttempt?
}
