package com.oponexis.companion.platform.callscreening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.os.SystemClock
import android.util.Log
import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.platform.calllifecycle.CallDirectionMemory

/**
 * M2 proof-of-concept service. It never blocks, rejects, silences, or performs I/O for a call.
 */
class OponexisCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val callbackStartedAtNanos = SystemClock.elapsedRealtimeNanos()
        val phoneNumber = callDetails.handle
            ?.takeIf {
                callDetails.handlePresentation == TelecomManager.PRESENTATION_ALLOWED &&
                    it.scheme == PhoneAccount.SCHEME_TEL
            }
            ?.schemeSpecificPart
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            CallDirectionMemory.record(phoneNumber, CallDirection.Outgoing)
            CallScreeningDiagnostics.recordOutgoingObservation()
            Log.i(LOG_TAG, "event=screening_observed direction=outgoing")
            CallerLookupDispatcher.dispatch(applicationContext, phoneNumber)
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
        CallDirectionMemory.record(phoneNumber, CallDirection.Incoming)
        val responseNanos = SystemClock.elapsedRealtimeNanos() - callbackStartedAtNanos
        CallScreeningDiagnostics.recordIncomingResponse(responseNanos)
        val snapshot = CallScreeningDiagnostics.snapshot.value
        Log.i(
            LOG_TAG,
            "event=screening_response direction=incoming " +
                "response_ms=${snapshot.lastResponseMillis} " +
                "within_internal_budget=${snapshot.lastResponseWithinBudget}",
        )

        CallerLookupDispatcher.dispatch(applicationContext, phoneNumber)
    }

    private companion object {
        const val LOG_TAG = "CallScreeningPoC"
    }
}
