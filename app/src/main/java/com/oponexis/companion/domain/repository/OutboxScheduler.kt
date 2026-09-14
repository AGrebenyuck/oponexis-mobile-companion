package com.oponexis.companion.domain.repository

interface OutboxScheduler {
    fun schedule(eventId: String)
}
