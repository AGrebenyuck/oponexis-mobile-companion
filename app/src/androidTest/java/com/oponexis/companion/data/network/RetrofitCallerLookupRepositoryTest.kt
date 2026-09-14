package com.oponexis.companion.data.network

import com.oponexis.companion.domain.model.CallerLookupFailure
import com.oponexis.companion.domain.model.CallerLookupResult
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RetrofitCallerLookupRepositoryTest {
    @Test
    fun mapsMatchedResponseAndSendsNormalizedNumber() = runBlocking {
        val api = FakeOpenXApi {
            Response.success(
                """{"result":"matched","match":{"customerRef":"customer-1","displayName":"Anna"}}"""
                    .toResponseBody(JSON_MEDIA_TYPE),
            )
        }
        val repository = configuredRepository(api)

        val result = repository.lookup("123 456 789")

        assertTrue(result is CallerLookupResult.Matched)
        result as CallerLookupResult.Matched
        assertEquals("customer-1", result.identity.customerRef)
        assertEquals("Anna", result.identity.displayName)
        assertEquals("Bearer test-token", api.authorization)
        assertEquals("+48123456789", JSONObject(requireNotNull(api.body)).getString("phoneNumber"))
        assertEquals(api.correlationId, JSONObject(requireNotNull(api.body)).getString("clientRequestId"))
    }

    @Test
    fun mapsNotFound() = runBlocking {
        val repository = configuredRepository(
            FakeOpenXApi {
                Response.success("""{"result":"not_found","match":null}""".toResponseBody(JSON_MEDIA_TYPE))
            },
        )

        assertEquals(CallerLookupResult.NotFound, repository.lookup("123456789"))
    }

    @Test
    fun mapsAuthThrottleServerAndNetworkFailures() = runBlocking {
        assertEquals(
            CallerLookupResult.Unauthorized,
            configuredRepository(FakeOpenXApi { errorResponse(401) }).lookup("123456789"),
        )
        assertEquals(
            CallerLookupResult.Unavailable(CallerLookupFailure.Throttled),
            configuredRepository(FakeOpenXApi { errorResponse(429) }).lookup("123456789"),
        )
        assertEquals(
            CallerLookupResult.Unavailable(CallerLookupFailure.Server),
            configuredRepository(FakeOpenXApi { errorResponse(503) }).lookup("123456789"),
        )
        assertEquals(
            CallerLookupResult.Unavailable(CallerLookupFailure.Network),
            configuredRepository(FakeOpenXApi { throw IOException("offline") }).lookup("123456789"),
        )
    }

    @Test
    fun rejectsMalformedSuccessfulResponse() = runBlocking {
        val repository = configuredRepository(
            FakeOpenXApi { Response.success("{}".toResponseBody(JSON_MEDIA_TYPE)) },
        )

        assertEquals(
            CallerLookupResult.Unavailable(CallerLookupFailure.InvalidResponse),
            repository.lookup("123456789"),
        )
    }

    @Test
    fun doesNotCallNetworkWhenConfigurationIsAbsent() = runBlocking {
        val api = FakeOpenXApi { error("must not be called") }
        val repository = RetrofitCallerLookupRepository(
            api = api,
            config = CrmNetworkConfig("https://invalid.local/", ""),
        )

        assertEquals(
            CallerLookupResult.Unavailable(CallerLookupFailure.NotConfigured),
            repository.lookup("123456789"),
        )
        assertEquals(0, api.callCount)
    }

    private fun configuredRepository(api: OpenXApi) = RetrofitCallerLookupRepository(
        api = api,
        config = CrmNetworkConfig("https://crm.test/", "test-token"),
    )

    private fun errorResponse(code: Int): Response<okhttp3.ResponseBody> =
        Response.error(code, "{}".toResponseBody(JSON_MEDIA_TYPE))

private class FakeOpenXApi(
        private val response: suspend () -> Response<okhttp3.ResponseBody>,
) : OpenXApi {
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
    ): Response<ResponseBody> = error("Not used")

        var callCount = 0
        var authorization: String? = null
        var correlationId: String? = null
        var body: String? = null

        override suspend fun lookupCaller(
            authorization: String,
            correlationId: String,
            body: RequestBody,
        ): Response<okhttp3.ResponseBody> {
            callCount += 1
            this.authorization = authorization
            this.correlationId = correlationId
            this.body = body.toBuffer().readUtf8()
            return response()
        }

        override suspend fun sendCallEvent(
            authorization: String,
            idempotencyKey: String,
            correlationId: String,
            body: RequestBody,
        ): Response<okhttp3.ResponseBody> = error("not used by caller lookup tests")

        override suspend fun sendSmsAction(
            authorization: String,
            correlationId: String,
            body: RequestBody,
        ): Response<okhttp3.ResponseBody> = error("not used by caller lookup tests")

        override suspend fun getSmsDeliveryStatus(
            authorization: String,
            receiptId: String,
            receiptType: String,
        ): Response<okhttp3.ResponseBody> = error("not used by caller lookup tests")

        override suspend fun getSmsTemplates(
            authorization: String,
            audience: String,
        ): Response<okhttp3.ResponseBody> = error("not used by caller lookup tests")

        override suspend fun createSmsTemplate(
            authorization: String,
            body: RequestBody,
        ): Response<okhttp3.ResponseBody> = error("not used by caller lookup tests")

        override suspend fun updateSmsTemplate(
            authorization: String,
            body: RequestBody,
        ): Response<okhttp3.ResponseBody> = error("not used by caller lookup tests")
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

private fun RequestBody.toBuffer() = okio.Buffer().also(::writeTo)
