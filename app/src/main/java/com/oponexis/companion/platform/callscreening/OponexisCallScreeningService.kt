package com.oponexis.companion.platform.callscreening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.os.SystemClock
import android.util.Log

/**
 * M2 proof-of-concept service. It never blocks, rejects, silences, or performs I/O for a call.
 */
class OponexisCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val callbackStartedAtNanos = SystemClock.elapsedRealtimeNanos()
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            CallScreeningDiagnostics.recordOutgoingObservation()
            Log.i(LOG_TAG, "event=screening_observed direction=outgoing")
            return
        }

        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build(),
        )
        val responseNanos = SystemClock.elapsedRealtimeNanos() - callbackStartedAtNanos
        CallScreeningDiagnostics.recordIncomingResponse(responseNanos)
        val snapshot = CallScreeningDiagnostics.snapshot.value
        Log.i(
            LOG_TAG,
            "event=screening_response direction=incoming " +
                "response_ms=${snapshot.lastResponseMillis} " +
                "within_internal_budget=${snapshot.lastResponseWithinBudget}",
        )
    }

    private companion object {
        const val LOG_TAG = "CallScreeningPoC"
    }
}
