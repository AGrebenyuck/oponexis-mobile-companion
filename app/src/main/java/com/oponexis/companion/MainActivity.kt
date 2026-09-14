package com.oponexis.companion

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oponexis.companion.ui.OponexisApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openCallOutcome = MutableStateFlow(false)
    private val openSmsScheduler = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openCallOutcome.value = intent.requestsCallOutcome()
        openSmsScheduler.value = intent.requestsSmsScheduler()
        enableEdgeToEdge()
        setContent {
            val openCallOutcomeRequested by openCallOutcome.collectAsStateWithLifecycle()
            val openSmsSchedulerRequested by openSmsScheduler.collectAsStateWithLifecycle()
            OponexisApp(
                openCallOutcomeRequested = openCallOutcomeRequested,
                onOpenCallOutcomeHandled = { openCallOutcome.value = false },
                openSmsSchedulerRequested = openSmsSchedulerRequested,
                onOpenSmsSchedulerHandled = { openSmsScheduler.value = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.requestsCallOutcome()) openCallOutcome.value = true
        if (intent.requestsSmsScheduler()) openSmsScheduler.value = true
    }

    private fun Intent.requestsCallOutcome(): Boolean =
        getBooleanExtra(EXTRA_OPEN_CALL_OUTCOME, false)

    private fun Intent.requestsSmsScheduler(): Boolean =
        getBooleanExtra(EXTRA_OPEN_SMS_SCHEDULER, false)

    companion object {
        const val EXTRA_OPEN_CALL_OUTCOME = "com.oponexis.companion.extra.OPEN_CALL_OUTCOME"
        const val EXTRA_OPEN_SMS_SCHEDULER = "com.oponexis.companion.extra.OPEN_SMS_SCHEDULER"
    }
}
