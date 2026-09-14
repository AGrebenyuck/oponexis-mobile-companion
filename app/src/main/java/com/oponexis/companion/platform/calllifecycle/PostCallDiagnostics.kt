package com.oponexis.companion.platform.calllifecycle

import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PostCallDisconnectCategory {
    Unknown,
    Local,
    Remote,
    Rejected,
    Missed,
    Other,
}

enum class PostCallDurationBucket {
    Unknown,
    VeryShort,
    Short,
    Medium,
    Long,
}

data class PostCallSnapshot(
    val observationCount: Long = 0,
    val lastDisconnectCategory: PostCallDisconnectCategory? = null,
    val lastDurationBucket: PostCallDurationBucket? = null,
)

object PostCallDiagnostics {
    private val mutableSnapshot = MutableStateFlow(PostCallSnapshot())
    val snapshot: StateFlow<PostCallSnapshot> = mutableSnapshot.asStateFlow()

    @Synchronized
    fun record(disconnectCause: Int, duration: Int) {
        val current = mutableSnapshot.value
        mutableSnapshot.value = current.copy(
            observationCount = current.observationCount + 1,
            lastDisconnectCategory = disconnectCause.toDisconnectCategory(),
            lastDurationBucket = duration.toDurationBucket(),
        )
    }

    internal fun resetForTest() {
        mutableSnapshot.value = PostCallSnapshot()
    }
}

internal fun Int.toDisconnectCategory(): PostCallDisconnectCategory = when (this) {
    DisconnectCause.UNKNOWN -> PostCallDisconnectCategory.Unknown
    DisconnectCause.LOCAL -> PostCallDisconnectCategory.Local
    DisconnectCause.REMOTE -> PostCallDisconnectCategory.Remote
    DisconnectCause.REJECTED -> PostCallDisconnectCategory.Rejected
    DisconnectCause.MISSED -> PostCallDisconnectCategory.Missed
    else -> PostCallDisconnectCategory.Other
}

internal fun Int.toDurationBucket(): PostCallDurationBucket = when (this) {
    TelecomManager.DURATION_VERY_SHORT -> PostCallDurationBucket.VeryShort
    TelecomManager.DURATION_SHORT -> PostCallDurationBucket.Short
    TelecomManager.DURATION_MEDIUM -> PostCallDurationBucket.Medium
    TelecomManager.DURATION_LONG -> PostCallDurationBucket.Long
    else -> PostCallDurationBucket.Unknown
}
