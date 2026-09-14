package com.oponexis.companion.data.network

import com.oponexis.companion.domain.model.CallerIdentity
import com.oponexis.companion.domain.model.CallerLookupFailure
import com.oponexis.companion.domain.model.CallerLookupResult
import com.oponexis.companion.domain.repository.CallerLookupRepository
import com.oponexis.companion.domain.repository.DiagnosticJournal
import com.oponexis.companion.domain.repository.NoOpDiagnosticJournal
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

@Singleton
class RetrofitCallerLookupRepository @Inject constructor(
    private val api: OpenXApi,
    private val config: CrmNetworkConfig,
    private val journal: DiagnosticJournal = NoOpDiagnosticJournal,
) : CallerLookupRepository {
    override suspend fun lookup(phoneNumber: String): CallerLookupResult {
        val normalizedPhone = PhoneNumberNormalizer.normalize(phoneNumber)
            ?: return CallerLookupResult.InvalidNumber
        if (!config.isConfigured) {
            return CallerLookupResult.Unavailable(CallerLookupFailure.NotConfigured)
        }

        val requestId = UUID.randomUUID().toString()
        val requestBody = JSONObject()
            .put("phoneNumber", normalizedPhone)
            .put("clientRequestId", requestId)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val result = try {
            val response = api.lookupCaller(
                authorization = "Bearer ${config.apiToken}",
                correlationId = requestId,
                body = requestBody,
            )
            when (response.code()) {
                200 -> parseSuccess(response.body()?.string())
                400 -> CallerLookupResult.InvalidNumber
                401, 403 -> CallerLookupResult.Unauthorized
                429 -> CallerLookupResult.Unavailable(CallerLookupFailure.Throttled)
                in 500..599 -> CallerLookupResult.Unavailable(CallerLookupFailure.Server)
                else -> CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse)
            }
        } catch (_: IOException) {
            CallerLookupResult.Unavailable(CallerLookupFailure.Network)
        }
        val reasonCode = when (result) {
            CallerLookupResult.Unauthorized -> "unauthorized"
            is CallerLookupResult.Unavailable -> result.reason.name.lowercase()
            else -> null
        }
        if (reasonCode != null) {
            journal.record(
                severity = DiagnosticSeverity.Warning,
                category = DiagnosticCategory.CallerLookup,
                outcome = DiagnosticOutcome.Failed,
                reasonCode = reasonCode,
                correlationId = requestId,
            )
        }
        return result
    }

    internal fun parseSuccess(rawBody: String?): CallerLookupResult {
        if (rawBody.isNullOrBlank()) {
            return CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse)
        }
        return try {
            val body = JSONObject(rawBody)
            when (body.optString("result")) {
                "not_found" -> CallerLookupResult.NotFound
                "matched" -> {
                    val match = body.optJSONObject("match")
                        ?: return CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse)
                    val customerRef = match.optString("customerRef").trim()
                    if (customerRef.isEmpty()) {
                        CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse)
                    } else {
                        CallerLookupResult.Matched(
                            CallerIdentity(
                                customerRef = customerRef,
                                displayName = match.optNullableString("displayName"),
								isReturningCustomer = match.optBoolean("isReturningCustomer", false),
								savedDetails = match.optNullableString("savedDetails"),
                            ),
                        )
                    }
                }
                else -> CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse)
            }
        } catch (_: JSONException) {
            CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    if (isNull(name)) null else optString(name).trim().ifEmpty { null }
