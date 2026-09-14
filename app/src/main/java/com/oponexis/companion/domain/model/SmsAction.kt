package com.oponexis.companion.domain.model

sealed interface SmsActionResult {
    data class Sent(val receiptId: String, val receiptType: String) : SmsActionResult
    data object InvalidPhone : SmsActionResult
    data object NotConfigured : SmsActionResult
    data object Unauthorized : SmsActionResult
    data class Failed(val detail: String? = null, val reasonCode: String? = null) : SmsActionResult
}

sealed interface SmsGatewayReadiness {
    data object Checking : SmsGatewayReadiness
    data class Ready(
        val profile: String,
        val deviceIdUsed: Boolean,
        val simNumber: Int?,
        val phoneNumber: String?,
        val deviceName: String?,
        val deviceLastSeen: String?,
    ) : SmsGatewayReadiness
    data object NotConfigured : SmsGatewayReadiness
    data object Unauthorized : SmsGatewayReadiness
    data class Unavailable(val reasonCode: String) : SmsGatewayReadiness
}

data class SmsTemplate(
    val id: String,
    val name: String,
    val kind: String,
    val audience: String,
    val body: String,
    val system: Boolean,
)

sealed interface SmsTemplateResult {
    data class Loaded(val templates: List<SmsTemplate>) : SmsTemplateResult
    data class Created(val template: SmsTemplate) : SmsTemplateResult
	data class Updated(val template: SmsTemplate) : SmsTemplateResult
    data class Failed(val detail: String) : SmsTemplateResult
}
