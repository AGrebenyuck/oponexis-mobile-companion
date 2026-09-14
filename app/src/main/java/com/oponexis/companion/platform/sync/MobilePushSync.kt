package com.oponexis.companion.platform.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oponexis.companion.data.local.CallOutcomeDao
import com.oponexis.companion.data.local.SmsActivityDao
import com.oponexis.companion.data.network.MobilePushRepository
import com.oponexis.companion.platform.notifications.SmsActivityNotificationManager
import com.oponexis.companion.platform.sms.DirectSmsDeviceIdentity
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import com.oponexis.companion.domain.repository.DiagnosticJournal
import com.oponexis.companion.domain.repository.SmsActionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class PushTokenRegistrationWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val installationId = inputData.getString(KEY_INSTALLATION_ID) ?: return Result.failure()
        val dependencies = dependencies()
        return if (dependencies.mobilePushRepository().registerInstallation(installationId)) Result.success() else Result.retry()
    }

    private fun dependencies(): MobilePushEntryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        MobilePushEntryPoint::class.java,
    )

    companion object {
        const val KEY_INSTALLATION_ID = "installation_id"
    }
}

class SmsActivitySyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val dependencies = dependencies()
        if (!dependencies.mobilePushRepository().syncActivity()) return Result.retry()
        inputData.getString(KEY_TRIGGER_EVENT_ID)?.let { eventId ->
            dependencies.diagnosticJournal().record(
                severity = DiagnosticSeverity.Info,
                category = DiagnosticCategory.PushSync,
                outcome = DiagnosticOutcome.Succeeded,
                correlationId = eventId,
            )
        }
        dependencies.smsActivityDao().recent()
            .distinctBy { it.providerMessageId ?: it.id }
            .forEach { event ->
                event.providerMessageId?.let { providerMessageId ->
                    if (event.status == "QUEUED" && shouldScheduleStallCheck(
                            event.occurredAtEpochMillis,
                            System.currentTimeMillis(),
                        )
                    ) {
                        dependencies.mobilePushScheduler().scheduleStallCheck(
                            providerMessageId,
                            event.occurredAtEpochMillis,
                        )
                    } else {
                        dependencies.mobilePushScheduler().cancelStallCheck(providerMessageId)
                    }
                }
            }
        return Result.success()
    }

    private fun dependencies(): MobilePushEntryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        MobilePushEntryPoint::class.java,
    )

    companion object {
        const val KEY_TRIGGER_EVENT_ID = "trigger_event_id"
    }
}

class SmsStallCheckWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val providerMessageId = inputData.getString(KEY_PROVIDER_MESSAGE_ID) ?: return Result.failure()
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            MobilePushEntryPoint::class.java,
        )
        dependencies.mobilePushRepository().syncActivity(providerMessageId)
        val latest = dependencies.smsActivityDao().latestForProvider(providerMessageId)
        if (latest?.status == "QUEUED") {
            dependencies.smsActivityNotifications().showStalled(providerMessageId, latest.phoneNumber)
        }
        return Result.success()
    }

    companion object {
        const val KEY_PROVIDER_MESSAGE_ID = "provider_message_id"
    }
}

class SmsReceiptStatusWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val callRef = inputData.getString(KEY_CALL_REF) ?: return Result.failure()
        val receiptId = inputData.getString(KEY_RECEIPT_ID) ?: return Result.failure()
        val receiptType = inputData.getString(KEY_RECEIPT_TYPE) ?: return Result.failure()
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            MobilePushEntryPoint::class.java,
        )
        val status = dependencies.smsActions().deliveryStatus(receiptId, receiptType)
            ?: return retryOrFinish()
        dependencies.callOutcomeDao().updateSmsDeliveryStatus(
            callRef,
            status.status,
            status.detail,
        )
        val notificationKey = status.providerMessageId ?: receiptId
        if (status.status in SMS_TERMINAL_STATUSES) {
            dependencies.smsActivityNotifications().showStatus(
                status.status,
                "COMPANION",
                notificationKey,
            )
            dependencies.mobilePushScheduler().syncActivity()
            return Result.success()
        }
        if (runAttemptCount == SMS_RECEIPT_WARNING_ATTEMPT) {
            val call = dependencies.callOutcomeDao().findByCallRef(callRef)
            dependencies.smsActivityNotifications().showStalled(
                notificationKey,
                call?.phoneNumber ?: call?.historyPhoneNumber,
            )
        }
        return retryOrFinish()
    }

    private fun retryOrFinish(): Result =
        if (runAttemptCount < SMS_RECEIPT_MAX_ATTEMPT) Result.retry() else Result.success()

    companion object {
        const val KEY_CALL_REF = "call_ref"
        const val KEY_RECEIPT_ID = "receipt_id"
        const val KEY_RECEIPT_TYPE = "receipt_type"
    }
}

@Singleton
class MobilePushSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun registerInstallation(installationId: String) {
        DirectSmsDeviceIdentity.save(context, installationId)
        val request = OneTimeWorkRequestBuilder<PushTokenRegistrationWorker>()
            .setInputData(
                Data.Builder()
                    .putString(PushTokenRegistrationWorker.KEY_INSTALLATION_ID, installationId)
                    .build(),
            )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            PUSH_TOKEN_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun syncActivity(triggerEventId: String? = null) {
        val request = OneTimeWorkRequestBuilder<SmsActivitySyncWorker>()
            .setInputData(
                Data.Builder()
                    .apply { triggerEventId?.let { putString(SmsActivitySyncWorker.KEY_TRIGGER_EVENT_ID, it) } }
                    .build(),
            )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SMS_ACTIVITY_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun trackSmsReceipt(callRef: String, receiptId: String, receiptType: String) {
        enqueueSmsReceiptCheck(
            callRef = callRef,
            receiptId = receiptId,
            receiptType = receiptType,
            initialDelayMillis = SMS_RECEIPT_INITIAL_DELAY_MILLIS,
            policy = ExistingWorkPolicy.REPLACE,
        )
    }

    private fun enqueueSmsReceiptCheck(
        callRef: String,
        receiptId: String,
        receiptType: String,
        initialDelayMillis: Long,
        policy: ExistingWorkPolicy,
    ) {
        val request = OneTimeWorkRequestBuilder<SmsReceiptStatusWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(SmsReceiptStatusWorker.KEY_CALL_REF, callRef)
                    .putString(SmsReceiptStatusWorker.KEY_RECEIPT_ID, receiptId)
                    .putString(SmsReceiptStatusWorker.KEY_RECEIPT_TYPE, receiptType)
                    .build(),
            )
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                SMS_RECEIPT_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            smsReceiptWorkName(callRef),
            policy,
            request,
        )
    }

    fun scheduleStallCheck(providerMessageId: String, queuedAtEpochMillis: Long) {
        val remainingDelay = stallDelayMillis(queuedAtEpochMillis, System.currentTimeMillis())
        val request = OneTimeWorkRequestBuilder<SmsStallCheckWorker>()
            .setInitialDelay(remainingDelay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(SmsStallCheckWorker.KEY_PROVIDER_MESSAGE_ID, providerMessageId)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            stallWorkName(providerMessageId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelStallCheck(providerMessageId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(stallWorkName(providerMessageId))
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun stallWorkName(providerMessageId: String) =
        "sms-stall-${providerMessageId.hashCode()}"

    private fun smsReceiptWorkName(callRef: String) =
        "sms-receipt-${callRef.hashCode()}"

    private companion object {
        const val PUSH_TOKEN_WORK = "firebase-push-token-registration"
        const val SMS_ACTIVITY_SYNC_WORK = "sms-activity-sync"
    }
}

internal const val SMS_STALL_AFTER_MILLIS = 4L * 60 * 1_000
internal const val SMS_STALL_SCHEDULE_WINDOW_MILLIS = 10L * 60 * 1_000
internal const val SMS_RECEIPT_INITIAL_DELAY_MILLIS = 20_000L
internal const val SMS_RECEIPT_BACKOFF_MILLIS = 15_000L
internal const val SMS_RECEIPT_WARNING_ATTEMPT = 4
internal const val SMS_RECEIPT_MAX_ATTEMPT = 7
internal val SMS_TERMINAL_STATUSES = setOf("SENT", "DELIVERED", "FAILED", "CANCELLED")

internal fun stallDelayMillis(queuedAtEpochMillis: Long, nowEpochMillis: Long): Long =
    (queuedAtEpochMillis + SMS_STALL_AFTER_MILLIS - nowEpochMillis).coerceAtLeast(0L)

internal fun shouldScheduleStallCheck(queuedAtEpochMillis: Long, nowEpochMillis: Long): Boolean =
    queuedAtEpochMillis <= nowEpochMillis &&
        nowEpochMillis - queuedAtEpochMillis <= SMS_STALL_SCHEDULE_WINDOW_MILLIS

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface MobilePushEntryPoint {
    fun mobilePushRepository(): MobilePushRepository
    fun smsActivityDao(): SmsActivityDao
    fun mobilePushScheduler(): MobilePushSyncScheduler
    fun smsActivityNotifications(): SmsActivityNotificationManager
    fun diagnosticJournal(): DiagnosticJournal
    fun smsActions(): SmsActionRepository
    fun callOutcomeDao(): CallOutcomeDao
}
