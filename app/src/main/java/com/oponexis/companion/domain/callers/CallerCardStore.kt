package com.oponexis.companion.domain.callers

import com.oponexis.companion.domain.model.CallerCardState
import com.oponexis.companion.domain.model.CallerLookupResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CallerCardStore @Inject constructor() {
    private val mutableState = MutableStateFlow<CallerCardState>(CallerCardState.Idle)
    val state: StateFlow<CallerCardState> = mutableState.asStateFlow()

    private var activeLookupId = 0L

    @Synchronized
    fun beginLookup(): Long {
        activeLookupId += 1
        mutableState.value = CallerCardState.Loading
        return activeLookupId
    }

    @Synchronized
    fun completeLookup(lookupId: Long, result: CallerLookupResult) {
        if (lookupId != activeLookupId) return
        mutableState.value = when (result) {
            is CallerLookupResult.Matched -> CallerCardState.Matched(result.identity)
            CallerLookupResult.NotFound -> CallerCardState.NotFound
            CallerLookupResult.InvalidNumber -> CallerCardState.NumberUnavailable
            CallerLookupResult.Unauthorized -> CallerCardState.Unauthorized
            is CallerLookupResult.Unavailable -> CallerCardState.Unavailable(result.reason)
        }
    }

    @Synchronized
    fun showNumberUnavailable() {
        activeLookupId += 1
        mutableState.value = CallerCardState.NumberUnavailable
    }

    @Synchronized
    fun clear() {
        activeLookupId += 1
        mutableState.value = CallerCardState.Idle
    }
}
