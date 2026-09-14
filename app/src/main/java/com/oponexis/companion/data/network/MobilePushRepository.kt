package com.oponexis.companion.data.network

import com.oponexis.companion.BuildConfig
import android.os.Build
import com.oponexis.companion.data.local.SmsActivityDao
import com.oponexis.companion.data.local.SmsActivityEntity
import com.oponexis.companion.data.local.CallOutcomeDao
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Singleton
class MobilePushRepository @Inject constructor(
    private val api: OpenXApi,
    private val config: CrmNetworkConfig,
    private val smsActivityDao: SmsActivityDao,
    private val callOutcomeDao: CallOutcomeDao,
) {
    suspend fun registerInstallation(installationId: String): Boolean {
        if (!config.isConfigured || installationId.isBlank()) return false
        val body = JSONObject()
            .put("installationId", installationId)
            .put("appId", BuildConfig.APPLICATION_ID)
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        return try {
            api.registerPushInstallation("Bearer ${config.apiToken}", body).isSuccessful
        } catch (_: IOException) {
            false
        }
    }

    suspend fun syncActivity(reconcileProviderMessageId: String? = null): Boolean {
        if (!config.isConfigured) return false
        return try {
            val response = api.getSmsActivity(
                "Bearer ${config.apiToken}",
                reconcileProviderMessageId,
            )
            if (!response.isSuccessful) return false
            val raw = response.body()?.string() ?: return false
            val array = JSONObject(raw).optJSONArray("items") ?: return false
            val items = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val status = item.optString("status").takeIf(String::isNotBlank) ?: continue
                    val occurredAt = runCatching {
                        Instant.parse(item.getString("occurredAt")).toEpochMilli()
                    }.getOrNull() ?: continue
                    add(
                        SmsActivityEntity(
                            id = id,
                            providerMessageId = item.optNullableString("providerMessageId"),
                            status = status,
                            source = item.optString("source", "PLATFORM"),
                            phoneNumber = item.optNullableString("phone"),
                            detail = item.optNullableString("detail"),
                            occurredAtEpochMillis = occurredAt,
                        ),
                    )
                }
            }
            smsActivityDao.upsertAll(items)
            items
                .distinctBy { it.providerMessageId ?: it.id }
                .filter { it.status != "QUEUED" }
                .forEach { item ->
                    item.providerMessageId?.let { providerMessageId ->
                        callOutcomeDao.updateSmsDeliveryStatusByReceipt(
                            providerMessageId,
                            item.status,
                            item.detail,
                        )
                    }
                }
            smsActivityDao.deleteBefore(System.currentTimeMillis() - ACTIVITY_RETENTION_MILLIS)
            true
        } catch (_: IOException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        takeUnless { isNull(key) }
            ?.optString(key)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val ACTIVITY_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1_000
    }
}
