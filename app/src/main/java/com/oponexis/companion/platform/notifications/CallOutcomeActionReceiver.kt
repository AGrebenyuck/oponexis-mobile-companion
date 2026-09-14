package com.oponexis.companion.platform.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oponexis.companion.domain.repository.CallOutcomeRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallOutcomeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callRef = intent.getStringExtra(EXTRA_CALL_REF)?.takeIf(String::isNotBlank) ?: return
        if (intent.action != ACTION_SKIP) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationActionEntryPoint::class.java,
                )
                val pending = entryPoint.callOutcomes().pending(callRef) ?: return@launch
                when (intent.action) {
                    ACTION_SKIP -> {
                        if (entryPoint.callOutcomes().dismiss(callRef)) {
                            entryPoint.notifications().cancel(callRef)
                        }
                    }
                }
            } catch (_: Exception) {
                Log.e(LOG_TAG, "event=notification_action_failed")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SKIP = "com.oponexis.companion.action.SKIP_CALL_OUTCOME"
        const val EXTRA_CALL_REF = "call_ref"
        private const val LOG_TAG = "CallAction"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface NotificationActionEntryPoint {
    fun callOutcomes(): CallOutcomeRepository
    fun notifications(): CallOutcomeNotificationManager
}
