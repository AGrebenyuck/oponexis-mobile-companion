package com.oponexis.companion.domain.callers

import com.oponexis.companion.domain.model.CallerLookupFailure
import com.oponexis.companion.domain.model.CallerLookupResult
import com.oponexis.companion.domain.network.NetworkRecoveryWaiter
import com.oponexis.companion.domain.repository.CallerLookupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class CallerLookupCoordinator @Inject constructor(
    private val repository: CallerLookupRepository,
    private val cardStore: CallerCardStore,
    private val networkRecoveryWaiter: NetworkRecoveryWaiter,
) {
    suspend fun lookup(phoneNumber: String?): CallerLookupResult? {
        if (phoneNumber.isNullOrBlank()) {
            cardStore.showNumberUnavailable()
            return null
        }
        val lookupId = cardStore.beginLookup()
        val firstResult = repository.lookup(phoneNumber)
        val result = when {
            firstResult.isNetworkFailure() && networkRecoveryWaiter.awaitRecovery() -> repository.lookup(phoneNumber)
            firstResult.isRetryable() -> retryOnce(phoneNumber, firstResult)
            else -> firstResult
        }
        cardStore.completeLookup(lookupId, result)
        return result
    }

    private suspend fun retryOnce(phoneNumber: String, firstResult: CallerLookupResult): CallerLookupResult {
        delay(1_000)
        val secondResult = repository.lookup(phoneNumber)
        if (!secondResult.isRetryable()) return secondResult

        delay(1_500)
        return repository.lookup(phoneNumber)
    }
}

private fun CallerLookupResult.isNetworkFailure(): Boolean =
    this is CallerLookupResult.Unavailable && reason == CallerLookupFailure.Network

private fun CallerLookupResult.isRetryable(): Boolean =
	this is CallerLookupResult.Unavailable && reason in setOf(
		CallerLookupFailure.Server,
		CallerLookupFailure.InvalidResponse,
	)
