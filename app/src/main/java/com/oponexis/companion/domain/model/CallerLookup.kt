package com.oponexis.companion.domain.model

data class CallerIdentity(
    val customerRef: String,
	val displayName: String?,
	val isReturningCustomer: Boolean = false,
	val savedDetails: String? = null,
)

sealed interface CallerLookupResult {
    data class Matched(val identity: CallerIdentity) : CallerLookupResult
    data object NotFound : CallerLookupResult
    data object InvalidNumber : CallerLookupResult
    data object Unauthorized : CallerLookupResult
    data class Unavailable(val reason: CallerLookupFailure) : CallerLookupResult
}

enum class CallerLookupFailure {
    NotConfigured,
    Network,
    Server,
    Throttled,
    InvalidResponse,
}
