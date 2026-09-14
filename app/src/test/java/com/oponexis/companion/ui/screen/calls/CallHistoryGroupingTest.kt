package com.oponexis.companion.ui.screen.calls

import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.model.CallState
import org.junit.Assert.assertEquals
import org.junit.Test

class CallHistoryGroupingTest {
    @Test
    fun groupsOnlyAdjacentCallsFromTheSameNumber() {
        val calls = listOf(
            call("latest-a", "+48 123 456 789"),
            call("older-a", "+48123456789"),
            call("middle-b", "+48987654321"),
            call("oldest-a", "+48123456789"),
        )

        val groups = groupConsecutiveCalls(calls)

        assertEquals(listOf(2, 1, 1), groups.map { it.count })
        assertEquals(listOf("latest-a", "middle-b", "oldest-a"), groups.map { it.representative.id })
    }

    @Test
    fun doesNotGroupCallsWithoutPhoneNumbers() {
        val groups = groupConsecutiveCalls(listOf(call("one", null), call("two", null)))

        assertEquals(listOf(1, 1), groups.map { it.count })
    }

    private fun call(id: String, phoneNumber: String?) = CallPreview(
        id = id,
        displayName = phoneNumber ?: "Unknown caller",
        phoneNumber = phoneNumber,
        company = null,
        smsReceiptId = null,
        smsReceiptType = null,
        smsDeliveryStatus = null,
        smsDeliveryDetail = null,
        observedAtEpochMillis = 1,
        direction = CallDirection.Incoming,
        state = CallState.Unknown,
    )
}
