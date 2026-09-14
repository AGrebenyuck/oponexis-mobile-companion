package com.oponexis.companion.domain.callers

import com.oponexis.companion.domain.model.CallerCardState
import com.oponexis.companion.domain.model.CallerLookupFailure
import com.oponexis.companion.domain.model.CallerLookupResult
import com.oponexis.companion.domain.network.NetworkRecoveryWaiter
import com.oponexis.companion.domain.repository.CallerLookupRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CallerLookupCoordinatorTest {
    @Test
    fun `lookup publishes repository result`() = runTest {
        val store = CallerCardStore()
        val coordinator = CallerLookupCoordinator(
            repository = FakeCallerLookupRepository(CallerLookupResult.NotFound),
            cardStore = store,
            networkRecoveryWaiter = FakeNetworkRecoveryWaiter(false),
        )

        coordinator.lookup("+48123456789")

        assertEquals(CallerCardState.NotFound, store.state.value)
    }

    @Test
    fun `missing number replaces previous card without network`() = runTest {
        val store = CallerCardStore()
        val repository = FakeCallerLookupRepository(CallerLookupResult.NotFound)
        val coordinator = CallerLookupCoordinator(repository, store, FakeNetworkRecoveryWaiter(false))

        coordinator.lookup(null)

        assertEquals(CallerCardState.NumberUnavailable, store.state.value)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `network failure retries after connectivity recovers`() = runTest {
        val store = CallerCardStore()
        val repository = SequencedCallerLookupRepository(
            CallerLookupResult.Unavailable(CallerLookupFailure.Network),
            CallerLookupResult.NotFound,
        )
        val waiter = FakeNetworkRecoveryWaiter(true)
        val coordinator = CallerLookupCoordinator(repository, store, waiter)

        coordinator.lookup("+48123456789")

        assertEquals(2, repository.callCount)
        assertEquals(1, waiter.callCount)
        assertEquals(CallerCardState.NotFound, store.state.value)
    }

    @Test
    fun `network failure completes without retry when connectivity does not recover`() = runTest {
        val store = CallerCardStore()
        val failure = CallerLookupResult.Unavailable(CallerLookupFailure.Network)
        val repository = FakeCallerLookupRepository(failure)
        val waiter = FakeNetworkRecoveryWaiter(false)
        val coordinator = CallerLookupCoordinator(repository, store, waiter)

        coordinator.lookup("+48123456789")

        assertEquals(1, repository.callCount)
        assertEquals(1, waiter.callCount)
        assertEquals(CallerCardState.Unavailable(CallerLookupFailure.Network), store.state.value)
    }

    @Test
    fun `server lookup is retried twice before the card is marked unavailable`() = runTest {
        val store = CallerCardStore()
        val repository = SequencedCallerLookupRepository(
            CallerLookupResult.Unavailable(CallerLookupFailure.Server),
            CallerLookupResult.Unavailable(CallerLookupFailure.Server),
            CallerLookupResult.NotFound,
        )
        val coordinator = CallerLookupCoordinator(repository, store, FakeNetworkRecoveryWaiter(false))

        coordinator.lookup("+48123456789")

        assertEquals(3, repository.callCount)
        assertEquals(CallerCardState.NotFound, store.state.value)
    }

    private class FakeCallerLookupRepository(
        private val result: CallerLookupResult,
    ) : CallerLookupRepository {
        var callCount = 0

        override suspend fun lookup(phoneNumber: String): CallerLookupResult {
            callCount += 1
            return result
        }
    }

    private class SequencedCallerLookupRepository(
        vararg results: CallerLookupResult,
    ) : CallerLookupRepository {
        private val results = ArrayDeque(results.toList())
        var callCount = 0

        override suspend fun lookup(phoneNumber: String): CallerLookupResult {
            callCount += 1
            return results.removeFirst()
        }
    }

    private class FakeNetworkRecoveryWaiter(
        private val recovered: Boolean,
    ) : NetworkRecoveryWaiter {
        var callCount = 0

        override suspend fun awaitRecovery(): Boolean {
            callCount += 1
            return recovered
        }
    }
}
