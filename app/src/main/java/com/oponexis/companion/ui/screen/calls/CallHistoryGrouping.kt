package com.oponexis.companion.ui.screen.calls

import com.oponexis.companion.domain.model.CallPreview

internal data class CallHistoryGroup(
    val calls: List<CallPreview>,
) {
    val representative: CallPreview = calls.first()
    val count: Int = calls.size
}

internal fun groupConsecutiveCalls(calls: List<CallPreview>): List<CallHistoryGroup> {
    if (calls.isEmpty()) return emptyList()

    val groups = mutableListOf<MutableList<CallPreview>>()
    calls.forEach { call ->
        val current = groups.lastOrNull()
        if (current != null && current.first().historyGroupingKey() == call.historyGroupingKey()) {
            current += call
        } else {
            groups += mutableListOf(call)
        }
    }
    return groups.map(::CallHistoryGroup)
}

private fun CallPreview.historyGroupingKey(): String {
    val digits = phoneNumber?.filter(Char::isDigit).orEmpty()
    return if (digits.isNotEmpty()) "phone:$digits" else "call:$id"
}
