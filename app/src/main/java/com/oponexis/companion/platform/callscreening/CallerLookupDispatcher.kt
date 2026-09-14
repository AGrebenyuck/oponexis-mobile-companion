package com.oponexis.companion.platform.callscreening

import android.content.Context
import com.oponexis.companion.domain.callers.CallerLookupCoordinator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object CallerLookupDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dispatch(applicationContext: Context, phoneNumber: String?) {
        scope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                CallerLookupEntryPoint::class.java,
            )
            entryPoint.coordinator().lookup(phoneNumber)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CallerLookupEntryPoint {
    fun coordinator(): CallerLookupCoordinator
}
