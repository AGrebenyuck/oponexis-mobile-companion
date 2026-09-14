package com.oponexis.companion.platform.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.oponexis.companion.domain.repository.OutboxScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerOutboxScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : OutboxScheduler {
    override fun schedule(eventId: String) {
        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setInputData(Data.Builder().putString(OutboxSyncWorker.KEY_EVENT_ID, eventId).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "outbox-$eventId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
