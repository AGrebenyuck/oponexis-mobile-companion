package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.CallerLookupResult

interface CallerLookupRepository {
    suspend fun lookup(phoneNumber: String): CallerLookupResult
}
