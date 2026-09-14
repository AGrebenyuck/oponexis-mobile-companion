package com.oponexis.companion.platform.calllifecycle

import android.os.Build
import android.os.Bundle
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import android.util.Log
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.RequiresApi

class PostCallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            intent?.action != TelecomManager.ACTION_POST_CALL
        ) {
            finish()
            return
        }

        val disconnectCause = intent.getIntExtra(
            TelecomManager.EXTRA_DISCONNECT_CAUSE,
            DisconnectCause.UNKNOWN,
        )
        val duration = intent.getIntExtra(
            TelecomManager.EXTRA_CALL_DURATION,
            UNKNOWN_DURATION,
        )
        val disconnectCategory = disconnectCause.toDisconnectCategory().name.lowercase()
        val durationBucket = duration.toDurationBucket().name.lowercase()
        val phoneNumber = intent.postCallHandle()?.toSafePhoneNumber()
        val direction = CallDirectionMemory.resolve(phoneNumber)
        PostCallDiagnostics.record(disconnectCause, duration)
        Log.i(
            LOG_TAG,
            "event=post_call_observed " +
                "disconnect=$disconnectCategory duration_bucket=$durationBucket",
        )
        PostCallOutcomeDispatcher.dispatch(
            applicationContext = applicationContext,
            disconnectCategory = disconnectCategory,
            durationBucket = durationBucket,
            phoneNumber = phoneNumber,
            direction = direction,
        )
        finish()
    }

    private companion object {
        const val LOG_TAG = "PostCallPoC"
        const val UNKNOWN_DURATION = -1
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun Intent.postCallHandle(): Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableExtra(TelecomManager.EXTRA_HANDLE, Uri::class.java)
} else {
    @Suppress("DEPRECATION")
    getParcelableExtra(TelecomManager.EXTRA_HANDLE)
}

private fun Uri.toSafePhoneNumber(): String? {
    if (scheme != "tel") return null
    val raw = schemeSpecificPart.trim()
    val digits = raw.filter(Char::isDigit)
    if (digits.isEmpty() || digits.length > 24) return null
    return if (raw.startsWith('+')) "+$digits" else digits
}
