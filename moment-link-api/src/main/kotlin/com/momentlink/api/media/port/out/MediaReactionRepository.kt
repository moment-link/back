package com.momentlink.api.media.port.out

import com.momentlink.domain.media.MediaReaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MediaReactionRepository : JpaRepository<MediaReaction, Long> {

    fun existsByMediaAssetIdAndParticipantSessionId(mediaAssetId: Long, participantSessionId: Long): Boolean

    @Modifying
    @Query("DELETE FROM MediaReaction r WHERE r.mediaAssetId = :mediaAssetId AND r.participantSessionId = :sessionId")
    fun deleteLike(mediaAssetId: Long, sessionId: Long): Int

    @Query("SELECT r.mediaAssetId FROM MediaReaction r WHERE r.participantSessionId = :sessionId AND r.mediaAssetId IN :mediaAssetIds")
    fun findLikedMediaIds(sessionId: Long, mediaAssetIds: Collection<Long>): List<Long>
}
