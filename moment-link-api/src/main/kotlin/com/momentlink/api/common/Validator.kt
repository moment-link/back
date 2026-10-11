package com.momentlink.api.common

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode

class Validator {

    companion object {
        fun pagination(page: Int, limit: Int) {
            if (page < 0 || limit !in 1..100) {
                throw BusinessException(ErrorCode.INVALID_PAGINATION)
            }
        }
    }
}