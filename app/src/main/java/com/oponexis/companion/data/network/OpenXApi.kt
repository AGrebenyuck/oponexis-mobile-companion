package com.oponexis.companion.data.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface OpenXApi {
    @GET("api/mobile/v1/sms-dispatches")
    suspend fun getSmsDispatches(
        @Header("Authorization") authorization: String,
        @Header("X-Installation-Id") installationId: String,
    ): Response<ResponseBody>

    @POST("api/mobile/v1/sms-dispatches")
    suspend fun reportSmsDispatch(
        @Header("Authorization") authorization: String,
        @Header("X-Installation-Id") installationId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/mobile/v1/push-registration")
    suspend fun registerPushInstallation(
        @Header("Authorization") authorization: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/mobile/v1/sms-activity")
    suspend fun getSmsActivity(
        @Header("Authorization") authorization: String,
        @Query("reconcileProviderMessageId") reconcileProviderMessageId: String? = null,
    ): Response<ResponseBody>

    @GET("api/mobile/v1/sms-gateway-health")
    suspend fun getSmsGatewayHealth(
        @Header("Authorization") authorization: String,
        @Header("X-Correlation-Id") correlationId: String,
    ): Response<ResponseBody>

    @POST("api/mobile/v1/caller-lookup")
    suspend fun lookupCaller(
        @Header("Authorization") authorization: String,
        @Header("X-Correlation-Id") correlationId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/mobile/v1/call-events")
    suspend fun sendCallEvent(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("X-Correlation-Id") correlationId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("api/mobile/v1/sms-actions")
    suspend fun sendSmsAction(
        @Header("Authorization") authorization: String,
        @Header("X-Correlation-Id") correlationId: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @GET("api/mobile/v1/sms-actions")
    suspend fun getSmsDeliveryStatus(
        @Header("Authorization") authorization: String,
        @Query("receiptId") receiptId: String,
        @Query("receiptType") receiptType: String,
    ): Response<ResponseBody>

    @GET("api/mobile/v1/sms-templates")
    suspend fun getSmsTemplates(
        @Header("Authorization") authorization: String,
        @Query("audience") audience: String,
    ): Response<ResponseBody>

    @POST("api/mobile/v1/sms-templates")
    suspend fun createSmsTemplate(
        @Header("Authorization") authorization: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

	@PUT("api/mobile/v1/sms-templates")
	suspend fun updateSmsTemplate(
		@Header("Authorization") authorization: String,
		@Body body: RequestBody,
	): Response<ResponseBody>
}
