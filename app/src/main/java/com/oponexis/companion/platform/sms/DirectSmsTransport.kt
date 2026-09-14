package com.oponexis.companion.platform.sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oponexis.companion.data.local.DirectSmsDao
import com.oponexis.companion.data.local.DirectSmsMessageEntity
import com.oponexis.companion.data.network.CrmNetworkConfig
import com.oponexis.companion.data.network.OpenXApi
import com.oponexis.companion.platform.notifications.SmsActivityNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val IDENTITY_PREFERENCES = "direct_sms_identity"
private const val INSTALLATION_ID_KEY = "installation_id"
private const val SENT_ACTION = "com.oponexis.companion.action.DIRECT_SMS_SENT"
private const val DELIVERED_ACTION = "com.oponexis.companion.action.DIRECT_SMS_DELIVERED"

object DirectSmsDeviceIdentity {
    fun save(context: Context, installationId: String) {
        context.getSharedPreferences(IDENTITY_PREFERENCES, Context.MODE_PRIVATE)
            .edit { putString(INSTALLATION_ID_KEY, installationId) }
    }

    fun read(context: Context): String? =
        context.getSharedPreferences(IDENTITY_PREFERENCES, Context.MODE_PRIVATE)
            .getString(INSTALLATION_ID_KEY, null)
            ?.takeIf { it.isNotBlank() }
}

class DirectSmsSyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val installationId = DirectSmsDeviceIdentity.read(applicationContext) ?: return Result.retry()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }
        val dependencies = dependencies()
        val config = dependencies.crmNetworkConfig()
        if (!config.isConfigured) return Result.retry()
        val response = runCatching {
            dependencies.openXApi().getSmsDispatches("Bearer ${config.apiToken}", installationId)
        }.getOrElse { return Result.retry() }
        if (!response.isSuccessful) return if (response.code() >= 500) Result.retry() else Result.failure()
        val items = JSONObject(response.body()?.string().orEmpty()).optJSONArray("items") ?: return Result.success()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optString("id")
            val phone = item.optString("phone")
            val body = item.optString("message")
            if (id.isBlank() || phone.isBlank() || body.isBlank()) continue
            val existing = dependencies.directSmsDao().find(id)
            if (existing != null) {
                if (existing.status in setOf("SENT", "DELIVERED", "FAILED")) {
                    report(dependencies, installationId, existing.id, existing.status, existing.error)
                }
                continue
            }
            send(dependencies, id, phone, body, item.optString("source", "PLATFORM"), item.optString("createdAt"))
        }
        return Result.success()
    }

    private suspend fun send(
        dependencies: DirectSmsEntryPoint,
        id: String,
        phone: String,
        body: String,
        source: String,
        createdAt: String,
    ) {
        val manager = applicationContext.getSystemService(SmsManager::class.java)
        val parts = manager.divideMessage(body).ifEmpty { arrayListOf(body) }
        val now = System.currentTimeMillis()
        dependencies.directSmsDao().insert(
            DirectSmsMessageEntity(
                id = id,
                direction = "OUT",
                phoneNumber = phone,
                body = body,
                status = "CLAIMED",
                source = source,
                createdAtEpochMillis = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(now),
                updatedAtEpochMillis = now,
                totalParts = parts.size,
            ),
        )
        try {
            val sent = ArrayList<PendingIntent>(parts.size)
            val delivered = ArrayList<PendingIntent>(parts.size)
            parts.indices.forEach { part ->
                sent += receiptIntent(SENT_ACTION, id, part)
                delivered += receiptIntent(DELIVERED_ACTION, id, part)
            }
            dependencies.directSmsDao().markSending(id, now)
            manager.sendMultipartTextMessage(phone, null, parts, sent, delivered)
        } catch (error: RuntimeException) {
            val detail = error.message?.take(300) ?: error.javaClass.simpleName
            dependencies.directSmsDao().markFailed(id, detail, System.currentTimeMillis())
            val installationId = DirectSmsDeviceIdentity.read(applicationContext) ?: return
            report(dependencies, installationId, id, "FAILED", detail)
        }
    }

    private fun receiptIntent(action: String, id: String, part: Int): PendingIntent {
        val intent = Intent(applicationContext, DirectSmsReceiptReceiver::class.java)
            .setAction(action)
            .setData("oponexis://sms/$id/$part/${action.hashCode()}".toUri())
            .putExtra("dispatch_id", id)
        return PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private suspend fun report(
        dependencies: DirectSmsEntryPoint,
        installationId: String,
        id: String,
        status: String,
        detail: String?,
    ): Boolean {
        val body = JSONObject().put("id", id).put("status", status).apply {
            if (!detail.isNullOrBlank()) put("detail", detail)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)
        return runCatching {
            dependencies.openXApi().reportSmsDispatch(
                "Bearer ${dependencies.crmNetworkConfig().apiToken}",
                installationId,
                body,
            ).isSuccessful
        }.getOrDefault(false)
    }

    private fun dependencies(): DirectSmsEntryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        DirectSmsEntryPoint::class.java,
    )
}

class DirectSmsReceiptWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("dispatch_id") ?: return Result.failure()
        val action = inputData.getString("receipt_action") ?: return Result.failure()
        val resultCode = inputData.getInt("result_code", SmsManager.RESULT_ERROR_GENERIC_FAILURE)
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, DirectSmsEntryPoint::class.java)
        val now = System.currentTimeMillis()
        if (resultCode != Activity.RESULT_OK) {
            val detail = smsError(resultCode)
            dependencies.directSmsDao().markFailed(id, detail, now)
        } else if (action == SENT_ACTION) {
            dependencies.directSmsDao().markPartSent(id, now)
        } else {
            dependencies.directSmsDao().markPartDelivered(id, now)
        }
        val message = dependencies.directSmsDao().find(id) ?: return Result.failure()
        if (message.status !in setOf("SENT", "DELIVERED", "FAILED")) return Result.success()
        val installationId = DirectSmsDeviceIdentity.read(applicationContext) ?: return Result.retry()
        val body = JSONObject().put("id", id).put("status", message.status).apply {
            message.error?.let { put("detail", it) }
        }.toString().toRequestBody(JSON_MEDIA_TYPE)
        val reported = runCatching {
            dependencies.openXApi().reportSmsDispatch(
                "Bearer ${dependencies.crmNetworkConfig().apiToken}", installationId, body,
            ).isSuccessful
        }.getOrDefault(false)
        if (!reported) return Result.retry()
        dependencies.smsNotifications().showStatus(message.status, message.source, "cmp:$id")
        return Result.success()
    }
}

class IncomingSmsUploadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val installationId = DirectSmsDeviceIdentity.read(applicationContext) ?: return Result.retry()
        val id = inputData.getString("message_id") ?: return Result.failure()
        val sender = inputData.getString("sender") ?: return Result.failure()
        val message = inputData.getString("message") ?: return Result.failure()
        val receivedAt = inputData.getString("received_at") ?: Instant.now().toString()
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, DirectSmsEntryPoint::class.java)
        dependencies.directSmsDao().insert(
            DirectSmsMessageEntity(
                id = "in:$id",
                direction = "IN",
                phoneNumber = sender,
                body = message,
                status = "RECEIVED",
                source = "COMPANION",
                createdAtEpochMillis = runCatching { Instant.parse(receivedAt).toEpochMilli() }.getOrDefault(System.currentTimeMillis()),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        val body = JSONObject()
            .put("event", "incoming")
            .put("messageId", id)
            .put("sender", sender)
            .put("message", message)
            .put("receivedAt", receivedAt)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val uploaded = runCatching {
            dependencies.openXApi().reportSmsDispatch(
                "Bearer ${dependencies.crmNetworkConfig().apiToken}", installationId, body,
            ).isSuccessful
        }.getOrDefault(false)
        return if (uploaded) Result.success() else Result.retry()
    }
}

class DirectSmsReceiptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("dispatch_id") ?: return
        DirectSmsScheduler.enqueueReceipt(context, id, intent.action.orEmpty(), resultCode)
    }
}

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, parts) ->
            if (sender.isBlank()) return@forEach
            val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
            val receivedAt = parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
            val stable = "$sender|$receivedAt|$body"
            val id = UUID.nameUUIDFromBytes(stable.toByteArray(StandardCharsets.UTF_8)).toString()
            DirectSmsScheduler.enqueueIncoming(context, id, sender, body, Instant.ofEpochMilli(receivedAt).toString())
        }
    }
}

@Singleton
class DirectSmsScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun sync() {
        val request = OneTimeWorkRequestBuilder<DirectSmsSyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("direct-sms-sync", ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        fun enqueueReceipt(context: Context, id: String, action: String, resultCode: Int) {
            val request = OneTimeWorkRequestBuilder<DirectSmsReceiptWorker>()
                .setInputData(Data.Builder().putString("dispatch_id", id).putString("receipt_action", action).putInt("result_code", resultCode).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun enqueueIncoming(context: Context, id: String, sender: String, message: String, receivedAt: String) {
            val request = OneTimeWorkRequestBuilder<IncomingSmsUploadWorker>()
                .setInputData(Data.Builder().putString("message_id", id).putString("sender", sender).putString("message", message).putString("received_at", receivedAt).build())
                .setConstraints(incomingSmsConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("incoming-sms-$id", ExistingWorkPolicy.KEEP, request)
        }

        private fun networkConstraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}

internal fun incomingSmsConstraints(): Constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
    .build()

private fun smsError(code: Int): String = when (code) {
    SmsManager.RESULT_ERROR_NO_SERVICE -> "Brak sieci komórkowej"
    SmsManager.RESULT_ERROR_RADIO_OFF -> "Moduł radiowy jest wyłączony"
    SmsManager.RESULT_ERROR_NULL_PDU -> "Operator odrzucił wiadomość"
    SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Przekroczono limit wysyłania SMS"
    else -> "Błąd wysyłania SMS ($code)"
}

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DirectSmsEntryPoint {
    fun openXApi(): OpenXApi
    fun crmNetworkConfig(): CrmNetworkConfig
    fun directSmsDao(): DirectSmsDao
    fun smsNotifications(): SmsActivityNotificationManager
}
