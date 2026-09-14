package com.oponexis.companion.data.network

import com.oponexis.companion.data.local.EventOutboxEntity
import com.oponexis.companion.domain.repository.CallEventDeliveryRepository
import com.oponexis.companion.domain.repository.CallEventDeliveryResult
import com.oponexis.companion.domain.repository.DiagnosticJournal
import com.oponexis.companion.domain.repository.NoOpDiagnosticJournal
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Singleton
class RetrofitCallEventDeliveryRepository @Inject constructor(
    private val api: OpenXApi,
    private val config: CrmNetworkConfig,
    private val journal: DiagnosticJournal = NoOpDiagnosticJournal,
) : CallEventDeliveryRepository {
    override suspend fun deliver(event: EventOutboxEntity): CallEventDeliveryResult {
        if (!config.isConfigured) return CallEventDeliveryResult.RetryableFailure("not_configured")
        val payload = JSONObject()
            .put("eventId", event.eventId)
            .put("schemaVersion", event.schemaVersion)
            .put("eventType", event.eventType)
            .put("callRef", event.callRef)
            .put("observedAt", Instant.ofEpochMilli(event.observedAtEpochMillis).toString())
            .put("resolvedAt", Instant.ofEpochMilli(event.resolvedAtEpochMillis).toString())
            .put("source", event.source)
            .put("confidence", event.confidence)
            .putNullable("phoneNumber", event.phoneNumber)
            .putNullable("customerRef", event.customerRef)
            .put(
                "attributes",
                JSONObject()
                    .put("disconnectCategory", event.disconnectCategory)
                    .put("durationBucket", event.durationBucket)
                    .put("outcomeCode", event.outcomeCode)
                    .putNullable("topicLabel", event.topicLabel),
            )
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val result = try {
            val response = api.sendCallEvent(
                authorization = "Bearer ${config.apiToken}",
                idempotencyKey = event.eventId,
                correlationId = event.eventId,
                body = payload,
            )
            when (response.code()) {
                200, 201 -> {
                    val receipt = runCatching {
                        JSONObject(response.body()?.string().orEmpty()).optString("receiptId")
                    }.getOrNull()?.takeIf(String::isNotBlank) ?: event.eventId
                    CallEventDeliveryResult.Delivered(receipt)
                }
                400, 401, 403, 409, 413, 422 ->
                    CallEventDeliveryResult.PermanentFailure("http_${response.code()}")
                429, in 500..599 -> CallEventDeliveryResult.RetryableFailure("http_${response.code()}")
                else -> CallEventDeliveryResult.PermanentFailure("http_unexpected")
            }
        } catch (_: IOException) {
            CallEventDeliveryResult.RetryableFailure("network")
        }
        if (result !is CallEventDeliveryResult.Delivered) {
            val reasonCode = when (result) {
                is CallEventDeliveryResult.PermanentFailure -> result.category
                is CallEventDeliveryResult.RetryableFailure -> result.category
                is CallEventDeliveryResult.Delivered -> null
            }
            journal.record(
                severity = DiagnosticSeverity.Warning,
                category = DiagnosticCategory.CallEventSync,
                outcome = DiagnosticOutcome.Failed,
                reasonCode = reasonCode,
                correlationId = event.eventId,
            )
        }
        return result
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun JSONObject.putNullable(name: String, value: String?): JSONObject =
    put(name, value ?: JSONObject.NULL)
