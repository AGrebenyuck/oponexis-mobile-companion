package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.SmsTemplateResult

interface SmsTemplateStore {
    suspend fun templates(audience: String): SmsTemplateResult

    suspend fun create(name: String, body: String, kind: String, audience: String): SmsTemplateResult

    suspend fun update(id: String, name: String, body: String, kind: String, audience: String): SmsTemplateResult
}

object NoOpSmsTemplateStore : SmsTemplateStore {
    override suspend fun templates(audience: String) = SmsTemplateResult.Failed("Local templates are unavailable.")

    override suspend fun create(name: String, body: String, kind: String, audience: String) =
        SmsTemplateResult.Failed("Local templates are unavailable.")

    override suspend fun update(id: String, name: String, body: String, kind: String, audience: String) =
        SmsTemplateResult.Failed("Local templates are unavailable.")
}
