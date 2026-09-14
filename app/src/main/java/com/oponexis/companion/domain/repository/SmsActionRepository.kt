package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.model.SmsActionResult
import com.oponexis.companion.domain.model.SmsTemplateResult
import com.oponexis.companion.domain.model.SmsGatewayReadiness

data class SmsDeliveryStatus(
    val status: String,
    val detail: String? = null,
    val providerMessageId: String? = null,
)

interface SmsActionRepository {
    suspend fun gatewayReadiness(): SmsGatewayReadiness

    suspend fun sendBookingForm(
        pending: PendingCallOutcome,
        visitDate: String? = null,
        visitTime: String? = null,
        messageOverride: String? = null,
    ): SmsActionResult

    suspend fun sendCustomMessage(
        pending: PendingCallOutcome,
        message: String,
    ): SmsActionResult

    suspend fun deliveryStatus(receiptId: String, receiptType: String): SmsDeliveryStatus?

    suspend fun templates(audience: String): SmsTemplateResult

    suspend fun createTemplate(
        name: String,
        body: String,
        kind: String,
        audience: String,
    ): SmsTemplateResult

	suspend fun updateTemplate(
		id: String,
		name: String,
		body: String,
		kind: String,
		audience: String,
	): SmsTemplateResult
}
