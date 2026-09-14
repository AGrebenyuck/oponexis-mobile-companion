package com.oponexis.companion.domain.callers

import com.oponexis.companion.domain.model.CallerCardState
import com.oponexis.companion.domain.model.CallerIdentity
import com.oponexis.companion.domain.model.CallerLookupResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CallerCardStoreTest {
    @Test
    fun `maps matched lookup to caller card`() {
        val store = CallerCardStore()
        val lookupId = store.beginLookup()

        store.completeLookup(
            lookupId,
            CallerLookupResult.Matched(CallerIdentity("customer-1", "Anna")),
        )

        assertEquals(
            CallerCardState.Matched(CallerIdentity("customer-1", "Anna")),
            store.state.value,
        )
    }

    @Test
    fun `late result cannot replace newer caller`() {
        val store = CallerCardStore()
        val oldLookup = store.beginLookup()
        val currentLookup = store.beginLookup()

        store.completeLookup(oldLookup, CallerLookupResult.NotFound)
        assertEquals(CallerCardState.Loading, store.state.value)

        store.completeLookup(currentLookup, CallerLookupResult.NotFound)
        assertEquals(CallerCardState.NotFound, store.state.value)
    }

    @Test
    fun `clear invalidates in flight result`() {
        val store = CallerCardStore()
        val lookupId = store.beginLookup()

        store.clear()
        store.completeLookup(lookupId, CallerLookupResult.NotFound)

        assertEquals(CallerCardState.Idle, store.state.value)
    }
}
