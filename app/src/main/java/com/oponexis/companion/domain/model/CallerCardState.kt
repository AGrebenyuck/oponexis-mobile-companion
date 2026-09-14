package com.oponexis.companion.domain.model

sealed interface CallerCardState {
    data object Idle : CallerCardState
    data object Loading : CallerCardState
    data class Matched(val identity: CallerIdentity) : CallerCardState
    data object NotFound : CallerCardState
    data object NumberUnavailable : CallerCardState
    data object Unauthorized : CallerCardState
    data class Unavailable(val reason: CallerLookupFailure) : CallerCardState
}
