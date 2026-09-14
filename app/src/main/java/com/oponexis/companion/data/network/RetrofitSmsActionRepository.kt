package com.oponexis.companion.data.network

import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.model.SmsActionResult
import com.oponexis.companion.domain.model.SmsGatewayReadiness
import com.oponexis.companion.domain.model.SmsTemplate
import com.oponexis.companion.domain.model.SmsTemplateResult
import com.oponexis.companion.domain.repository.SmsActionRepository
import com.oponexis.companion.domain.repository.SmsDeliveryStatus
import com.oponexis.companion.domain.repository.DiagnosticJournal
import com.oponexis.companion.domain.repository.NoOpDiagnosticJournal
import com.oponexis.companion.domain.repository.NoOpSmsTemplateStore
import com.oponexis.companion.domain.repository.SmsTemplateStore
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import com.oponexis.companion.domain.network.NetworkRecoveryWaiter
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class RetrofitSmsActionRepository @Inject constructor(
    private val api: OpenXApi,
    private val config: CrmNetworkConfig,
    private val journal: DiagnosticJournal = NoOpDiagnosticJournal,
    private val templateStore: SmsTemplateStore = NoOpSmsTemplateStore,
    private val deviceSmsReadiness: DeviceSmsReadiness = DeviceSmsReadiness { null },
    private val networkRecoveryWaiter: NetworkRecoveryWaiter = NetworkRecoveryWaiter { true },
) : SmsActionRepository {
    override suspend fun gatewayReadiness(): SmsGatewayReadiness {
        val correlationId = UUID.randomUUID().toString()
        val localUnavailable = deviceSmsReadiness.unavailableReason()
        if (localUnavailable != null) {
            recordGatewayFailure(localUnavailable, correlationId)
            return SmsGatewayReadiness.Unavailable(localUnavailable)
        }
        if (!config.isConfigured) {
            recordGatewayFailure("mobile_api_not_configured", correlationId)
            return SmsGatewayReadiness.NotConfigured
        }
        val networkReady = withTimeoutOrNull(15_000L) {
            networkRecoveryWaiter.awaitRecovery()
        } == true
        if (!networkReady) {
            recordGatewayFailure("network", correlationId)
            return SmsGatewayReadiness.Unavailable("network")
        }
        return try {
            val response = api.getSmsGatewayHealth(
                authorization = "Bearer ${config.apiToken}",
                correlationId = correlationId,
            )
            when (response.code()) {
                200 -> {
                    val body = response.body()?.string()
                    val deviceLastSeen = jsonString(body, "deviceLastSeen")
                    val profile = jsonString(body, "profile") ?: "default"
                    if (jsonString(body, "status") != "ready" || deviceLastSeen == null) {
                        recordGatewayFailure("invalid_response", correlationId)
                        SmsGatewayReadiness.Unavailable("invalid_response")
                    } else {
                        journal.record(
                            severity = DiagnosticSeverity.Info,
                            category = DiagnosticCategory.SmsGateway,
                            outcome = DiagnosticOutcome.Succeeded,
                            correlationId = correlationId,
                        )
                        SmsGatewayReadiness.Ready(
                            profile = profile,
                            deviceIdUsed = jsonBoolean(body, "deviceIdUsed") ?: false,
                            simNumber = jsonInt(body, "simNumber")?.takeIf { it >= 0 },
                            phoneNumber = jsonString(body, "phoneNumber"),
                            deviceName = jsonString(body, "deviceName"),
                            deviceLastSeen = deviceLastSeen,
                        )
                    }
                }
                401, 403 -> {
                    recordGatewayFailure("unauthorized", correlationId)
                    SmsGatewayReadiness.Unauthorized
                }
                503 -> {
                    val code = errorCode(response.errorBody()?.string()) ?: "sms_gateway_unavailable"
                    recordGatewayFailure(code, correlationId)
                    if (code.endsWith("not_configured")) SmsGatewayReadiness.NotConfigured
                    else SmsGatewayReadiness.Unavailable(code)
                }
                else -> {
                    val code = "http_${response.code()}"
                    recordGatewayFailure(code, correlationId)
                    SmsGatewayReadiness.Unavailable(code)
                }
            }
        } catch (_: IOException) {
            recordGatewayFailure("network", correlationId)
            SmsGatewayReadiness.Unavailable("network")
        }
    }

    override suspend fun sendBookingForm(
        pending: PendingCallOutcome,
        visitDate: String?,
        visitTime: String?,
        messageOverride: String?,
    ): SmsActionResult {
        val phone = pending.phoneNumber?.let(PhoneNumberNormalizer::normalize)
            ?: return SmsActionResult.InvalidPhone
        if (!config.isConfigured) return SmsActionResult.NotConfigured

        val requestId = pending.callRef
        val displayName = pending.displayName?.let { "\"${jsonEscape(it)}\"" } ?: "null"
        val dateJson = visitDate?.let { "\"${jsonEscape(it)}\"" } ?: "null"
        val timeJson = visitTime?.let { "\"${jsonEscape(it)}\"" } ?: "null"
        val messageJson = messageOverride?.let { "\"${jsonEscape(it)}\"" } ?: "null"
        val body = (
            "{" +
                "\"requestId\":\"${jsonEscape(requestId)}\"," +
                "\"action\":\"send_booking_form\"," +
                "\"phoneNumber\":\"${jsonEscape(phone)}\"," +
                "\"displayName\":$displayName," +
                "\"visitDate\":$dateJson," +
                "\"visitTime\":$timeJson," +
                "\"messageOverride\":$messageJson" +
                "}"
            ).toRequestBody(JSON_MEDIA_TYPE)

        return sendRequest(requestId, body)
    }

    override suspend fun sendCustomMessage(
        pending: PendingCallOutcome,
        message: String,
    ): SmsActionResult {
        val phone = pending.phoneNumber?.let(PhoneNumberNormalizer::normalize)
            ?: return SmsActionResult.InvalidPhone
        if (!config.isConfigured) return SmsActionResult.NotConfigured
        if (message.isBlank()) return SmsActionResult.Failed("Enter a message before sending.", "invalid_message")

        val requestId = UUID.randomUUID().toString()
        val body = (
            "{" +
                "\"requestId\":\"${jsonEscape(requestId)}\"," +
                "\"action\":\"send_custom_message\"," +
                "\"phoneNumber\":\"${jsonEscape(phone)}\"," +
                "\"message\":\"${jsonEscape(message.trim())}\"" +
                "}"
            ).toRequestBody(JSON_MEDIA_TYPE)

        return sendRequest(requestId, body)
    }

    override suspend fun deliveryStatus(receiptId: String, receiptType: String): SmsDeliveryStatus? {
        if (!config.isConfigured) return null
        return try {
            val response = api.getSmsDeliveryStatus(
                authorization = "Bearer ${config.apiToken}",
                receiptId = receiptId,
                receiptType = receiptType,
            )
            if (!response.isSuccessful) return null
            val body = response.body()?.string()
            val status = jsonString(body, "status") ?: return null
            SmsDeliveryStatus(
                status = status,
                detail = jsonString(body, "detail"),
                providerMessageId = jsonString(body, "providerMessageId"),
            )
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun templates(audience: String): SmsTemplateResult =
        templateStore.templates(audience)

    override suspend fun createTemplate(
        name: String,
        body: String,
        kind: String,
        audience: String,
    ): SmsTemplateResult = templateStore.create(name, body, kind, audience)

	override suspend fun updateTemplate(
		id: String,
		name: String,
		body: String,
		kind: String,
		audience: String,
	): SmsTemplateResult = templateStore.update(id, name, body, kind, audience)

    private suspend fun sendRequest(requestId: String, body: okhttp3.RequestBody): SmsActionResult {
        val result = try {
            val response = api.sendSmsAction(
                authorization = "Bearer ${config.apiToken}",
                correlationId = requestId,
                body = body,
            )
            when (response.code()) {
                200, 201 -> {
                    val responseBody = response.body()?.string()
                    val receiptId = jsonString(responseBody, "receiptId")
                    val receiptType = jsonString(responseBody, "receiptType")
                    if (receiptId != null && receiptType != null) {
                        SmsActionResult.Sent(receiptId, receiptType)
                    } else {
                        SmsActionResult.Failed("CRM did not return an SMS receipt. Try again.", "invalid_receipt")
                    }
                }
                400, 422 -> SmsActionResult.InvalidPhone
                401, 403 -> SmsActionResult.Unauthorized
                503 -> when (errorCode(response.errorBody()?.string())) {
                    "sms_gateway_not_configured", "mobile_api_not_configured" ->
                        SmsActionResult.NotConfigured
                    else -> SmsActionResult.Failed("CRM is temporarily unavailable.", "service_unavailable")
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    SmsActionResult.Failed(errorDetail(errorBody), errorCode(errorBody) ?: "http_${response.code()}")
                }
            }
        } catch (_: IOException) {
            SmsActionResult.Failed("Cannot reach CRM. Check the CRM server and phone network.", "network")
        }
        journal.record(
            severity = if (result is SmsActionResult.Sent) DiagnosticSeverity.Info else DiagnosticSeverity.Error,
            category = DiagnosticCategory.SmsSend,
            outcome = if (result is SmsActionResult.Sent) DiagnosticOutcome.Succeeded else DiagnosticOutcome.Failed,
            reasonCode = when (result) {
                is SmsActionResult.Sent -> null
                SmsActionResult.InvalidPhone -> "invalid_phone"
                SmsActionResult.NotConfigured -> "not_configured"
                SmsActionResult.Unauthorized -> "unauthorized"
                is SmsActionResult.Failed -> result.reasonCode ?: "send_failed"
            },
            correlationId = requestId,
        )
        return result
    }

    private suspend fun recordGatewayFailure(reasonCode: String, correlationId: String) {
        journal.record(
            severity = DiagnosticSeverity.Error,
            category = DiagnosticCategory.SmsGateway,
            outcome = DiagnosticOutcome.Failed,
            reasonCode = reasonCode,
            correlationId = correlationId,
        )
    }

    private fun errorCode(rawBody: String?): String? = jsonString(rawBody, "code")

    private fun errorDetail(rawBody: String?): String? {
        return jsonString(rawBody, "detail")
            ?: jsonString(rawBody, "code")?.replace('_', ' ')
    }

    private fun jsonString(rawBody: String?, key: String): String? = rawBody
        ?.let { body ->
            Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
                .find(body)
                ?.groupValues
                ?.get(1)
        }
        ?.replace("\\n", "\n")
        ?.replace("\\r", "\r")
        ?.replace("\\t", "\t")
        ?.replace("\\\"", "\"")
        ?.replace("\\\\", "\\")
        ?.takeIf(String::isNotBlank)

    private fun jsonBoolean(rawBody: String?, key: String): Boolean? = rawBody
        ?.let { body ->
            Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*(true|false)")
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toBooleanStrictOrNull()
        }

    private fun jsonInt(rawBody: String?, key: String): Int? = rawBody
        ?.let { body ->
            Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*(-?\\d+)")
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun jsonEscape(value: String): String = buildString(value.length) {
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u")
                append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
}
