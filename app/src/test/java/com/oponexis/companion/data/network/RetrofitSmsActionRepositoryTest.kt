package com.oponexis.companion.data.network

import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.model.SmsActionResult
import com.oponexis.companion.domain.model.SmsGatewayReadiness
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RetrofitSmsActionRepositoryTest {
    @Test
    fun `reports live gateway readiness`() = runTest {
        val repository = RetrofitSmsActionRepository(
            api = FakeOpenXApi(
                healthResponse = Response.success(
                    """{"result":"ok","status":"ready","profile":"form","deviceIdUsed":true,"simNumber":1,"phoneNumber":"+48123456789","deviceName":"Work phone","deviceLastSeen":"2026-08-14T12:00:00Z"}"""
                        .toResponseBody(),
                ),
            ),
            config = CrmNetworkConfig("https://crm.example/", "dev-token"),
        )

        assertEquals(
            SmsGatewayReadiness.Ready(
                profile = "form",
                deviceIdUsed = true,
                simNumber = 1,
                phoneNumber = "+48123456789",
                deviceName = "Work phone",
                deviceLastSeen = "2026-08-14T12:00:00Z",
            ),
            repository.gatewayReadiness(),
        )
    }

    @Test
    fun `rejects legacy readiness without live device evidence`() = runTest {
        val repository = RetrofitSmsActionRepository(
            api = FakeOpenXApi(
                healthResponse = Response.success(
                    """{"result":"ok","status":"ready","profile":"form","deviceIdUsed":true,"simNumber":1}"""
                        .toResponseBody(),
                ),
            ),
            config = CrmNetworkConfig("https://crm.example/", "dev-token"),
        )

        assertEquals(
            SmsGatewayReadiness.Unavailable("invalid_response"),
            repository.gatewayReadiness(),
        )
    }

    @Test
    fun `sends booking action with stable call reference idempotency key`() = runTest {
        val api = FakeOpenXApi()
        val repository = RetrofitSmsActionRepository(
            api = api,
            config = CrmNetworkConfig("https://crm.example/", "dev-token"),
        )
        val pending = PendingCallOutcome(
            callRef = "123e4567-e89b-12d3-a456-426614174000",
            observedAtEpochMillis = 1,
            disconnectCategory = "remote",
            durationBucket = "short",
            phoneNumber = "+48 123 456 789",
            displayName = "Jan",
        )

        assertEquals(
            SmsActionResult.Sent("42", "form"),
            repository.sendBookingForm(pending, "2026-07-26", "10:30"),
        )
		assertEquals(
			"{\"requestId\":\"${pending.callRef}\",\"action\":\"send_booking_form\"," +
				"\"phoneNumber\":\"+48123456789\",\"displayName\":\"Jan\"," +
				"\"visitDate\":\"2026-07-26\",\"visitTime\":\"10:30\",\"messageOverride\":null}",
			api.smsBody,
		)
        assertEquals(pending.callRef, api.smsCorrelationId)
    }

    @Test
    fun `rejects missing phone without network request`() = runTest {
        val api = FakeOpenXApi()
        val repository = RetrofitSmsActionRepository(
            api = api,
            config = CrmNetworkConfig("https://crm.example/", "dev-token"),
        )

        assertEquals(
            SmsActionResult.InvalidPhone,
            repository.sendBookingForm(
                PendingCallOutcome("call", 1, "remote", "short", null, null),
                null,
                null,
            ),
        )
        assertEquals(null, api.smsBody)
    }

    @Test
    fun `shows gateway failure detail returned by CRM`() = runTest {
        val api = FakeOpenXApi(
            smsResponse = Response.error(
                502,
                """{"result":"error","error":{"code":"sms_gateway_rejected","detail":"Device is offline"}}"""
                    .toResponseBody(),
            ),
        )
        val repository = RetrofitSmsActionRepository(
            api = api,
            config = CrmNetworkConfig("https://crm.example/", "dev-token"),
        )

        assertEquals(
            SmsActionResult.Failed("Device is offline", "sms_gateway_rejected"),
            repository.sendBookingForm(
                PendingCallOutcome("call", 1, "remote", "short", "+48123456789", null),
                null,
                null,
            ),
        )
    }

    @Test
    fun `sends a custom message through the mobile SMS action`() = runTest {
        val api = FakeOpenXApi(
            smsResponse = Response.success(
                "{\"receiptId\":\"gateway-1\",\"receiptType\":\"message\"}".toResponseBody(),
            ),
        )
        val repository = RetrofitSmsActionRepository(
            api = api,
            config = CrmNetworkConfig("https://crm.example/", "dev-token"),
        )
        val pending = PendingCallOutcome(
            "custom-call",
            1,
            "remote",
            "short",
            "+48123456789",
            null,
        )

        assertEquals(SmsActionResult.Sent("gateway-1", "message"), repository.sendCustomMessage(pending, "Oddzwonimy jutro."))
        assertTrue(api.smsBody?.contains("\"action\":\"send_custom_message\"") == true)
        assertTrue(api.smsBody?.contains("\"phoneNumber\":\"+48123456789\"") == true)
        assertTrue(api.smsBody?.contains("\"message\":\"Oddzwonimy jutro.\"") == true)
        assertTrue(api.smsBody?.contains("\"requestId\":\"custom-call\"") == false)
    }
}

private class FakeOpenXApi(
    private val smsResponse: Response<ResponseBody> = Response.success(
        "{\"receiptId\":\"42\",\"receiptType\":\"form\"}".toResponseBody(),
    ),
    private val healthResponse: Response<ResponseBody> = Response.success(
        """{"result":"ok","status":"ready","profile":"form","deviceIdUsed":true}""".toResponseBody(),
    ),
) : OpenXApi {
    var smsBody: String? = null
    var smsCorrelationId: String? = null

    override suspend fun getSmsDispatches(
        authorization: String,
        installationId: String,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun reportSmsDispatch(
        authorization: String,
        installationId: String,
        body: RequestBody,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun registerPushInstallation(
        authorization: String,
        body: RequestBody,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun getSmsActivity(
        authorization: String,
        reconcileProviderMessageId: String?,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun getSmsGatewayHealth(
        authorization: String,
        correlationId: String,
    ): Response<ResponseBody> = healthResponse

    override suspend fun lookupCaller(
        authorization: String,
        correlationId: String,
        body: RequestBody,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun sendCallEvent(
        authorization: String,
        idempotencyKey: String,
        correlationId: String,
        body: RequestBody,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun sendSmsAction(
        authorization: String,
        correlationId: String,
        body: RequestBody,
    ): Response<ResponseBody> {
        smsCorrelationId = correlationId
        smsBody = Buffer().also(body::writeTo).readUtf8()
		return smsResponse
    }

    override suspend fun getSmsDeliveryStatus(
        authorization: String,
        receiptId: String,
        receiptType: String,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun getSmsTemplates(
        authorization: String,
        audience: String,
    ): Response<ResponseBody> = error("Not used")

    override suspend fun createSmsTemplate(
        authorization: String,
        body: RequestBody,
    ): Response<ResponseBody> = error("Not used")

	override suspend fun updateSmsTemplate(
		authorization: String,
		body: RequestBody,
	): Response<ResponseBody> = error("Not used")
}
