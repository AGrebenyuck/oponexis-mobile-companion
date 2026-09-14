package com.oponexis.companion.platform.calllifecycle

import com.oponexis.companion.domain.model.CallDirection
import java.util.concurrent.ConcurrentHashMap

internal object CallDirectionMemory {
    private const val MAX_AGE_MILLIS = 5 * 60 * 1_000L
    private val directions = ConcurrentHashMap<String, Pair<CallDirection, Long>>()

    fun record(phoneNumber: String?, direction: CallDirection) {
        val phone = phoneNumber?.trim()?.takeIf(String::isNotEmpty) ?: return
        directions[phone] = direction to System.currentTimeMillis()
    }

    fun resolve(phoneNumber: String?): CallDirection {
        val phone = phoneNumber?.trim()?.takeIf(String::isNotEmpty) ?: return CallDirection.Incoming
        val entry = directions.remove(phone) ?: return CallDirection.Incoming
        return if (System.currentTimeMillis() - entry.second <= MAX_AGE_MILLIS) entry.first else CallDirection.Incoming
    }
}
