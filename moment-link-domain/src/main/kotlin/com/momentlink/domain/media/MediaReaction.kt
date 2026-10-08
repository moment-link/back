package com.momentlink.domain.media

import com.momentlink.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "media_reaction",
    indexes = [
        Index(name = "idx_media_reaction_id", columnList = "media_asset_id,participant_session_id")
    ]
)
class MediaReaction private constructor(
    mediaAssetId: Long,
    participantSessionId: Long,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "media_asset_id")
    var mediaAssetId: Long? = mediaAssetId
        protected set

    @Column(name = "participant_session_id", nullable = false, updatable = false)
    var participantSessionId: Long? = participantSessionId
        protected set

    init {
        require(mediaAssetId > 0)
        require(participantSessionId > 0)
    }

    companion object {
        fun create(
            mediaAssetId: Long,
            participantSessionId: Long,
        ): MediaReaction = MediaReaction(
            mediaAssetId,
            participantSessionId
        )
    }
}