package com.momentlink.api.event.port.out

import com.momentlink.domain.event.PublicAccess
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PublicAccessRepository: JpaRepository<PublicAccess, Long> {

    fun findByAccessKeyHash(accessKeyHash: String): PublicAccess?
}