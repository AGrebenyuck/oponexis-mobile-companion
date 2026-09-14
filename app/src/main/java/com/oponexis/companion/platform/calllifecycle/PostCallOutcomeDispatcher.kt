package com.oponexis.companion.platform.calllifecycle

import android.content.Context
import android.util.Log
import com.oponexis.companion.domain.repository.CallOutcomeRepository
import com.oponexis.companion.domain.callers.CallerLookupCoordinator
import com.oponexis.companion.domain.model.CallerLookupResult
import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.platform.notifications.CallOutcomeNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object PostCallOutcomeDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dispatch(
        applicationContext: Context,
        disconnectCategory: String,
        durationBucket: String,
        phoneNumber: String?,
		direction: CallDirection,
    ) {
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    PostCallOutcomeEntryPoint::class.java,
                )
                val pending = entryPoint.repository().createPending(
                    disconnectCategory = disconnectCategory,
                    durationBucket = durationBucket,
                    phoneNumber = phoneNumber,
					direction = direction,
                )
                entryPoint.notifications().show(pending)
                Log.i(LOG_TAG, "event=outcome_draft_created")
                if (phoneNumber != null) {
                    val lookup = entryPoint.callerLookupCoordinator().lookup(phoneNumber)
                    if (lookup == CallerLookupResult.NotFound) {
                        entryPoint.repository().clearIdentityForPhone(phoneNumber)
                    }
                    val identity = (lookup as? CallerLookupResult.Matched)?.identity
                    val displayName = identity?.displayName?.trim()?.takeIf(String::isNotEmpty)
                    if (identity != null &&
                        entryPoint.repository().attachIdentityForPhone(
							phoneNumber = phoneNumber,
                            displayName = displayName,
                            customerRef = identity.customerRef,
							isReturningCustomer = identity.isReturningCustomer,
                        )
                    ) {
                        entryPoint.notifications().show(pending.copy(displayName = displayName))
                    }
                }
            } catch (_: Exception) {
                Log.e(LOG_TAG, "event=outcome_draft_failed category=storage")
            }
        }
    }

    private const val LOG_TAG = "PostCallPoC"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PostCallOutcomeEntryPoint {
    fun repository(): CallOutcomeRepository

    fun notifications(): CallOutcomeNotificationManager

    fun callerLookupCoordinator(): CallerLookupCoordinator
}
