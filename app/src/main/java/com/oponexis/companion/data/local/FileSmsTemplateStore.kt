package com.oponexis.companion.data.local

import android.content.Context
import android.util.AtomicFile
import com.oponexis.companion.domain.model.SmsTemplate
import com.oponexis.companion.domain.model.SmsTemplateResult
import com.oponexis.companion.domain.repository.SmsTemplateStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class FileSmsTemplateStore @Inject constructor(
    @ApplicationContext context: Context,
) : SmsTemplateStore {
    private val file = AtomicFile(context.filesDir.resolve(FILE_NAME))
    private val mutex = Mutex()

    override suspend fun templates(audience: String): SmsTemplateResult = mutex.withLock {
        SmsTemplateResult.Loaded(loadTemplates().filter { it.audience == audience || it.audience == "ALL" })
    }

    override suspend fun create(
        name: String,
        body: String,
        kind: String,
        audience: String,
    ): SmsTemplateResult = mutex.withLock {
        validationError(name, body, kind, audience)?.let { return@withLock SmsTemplateResult.Failed(it) }
        val template = SmsTemplate(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            kind = kind,
            audience = audience,
            body = body.trim(),
            system = false,
        )
        val updated = loadTemplates() + template
        if (!writeTemplates(updated)) return@withLock SmsTemplateResult.Failed("Cannot save the local template.")
        SmsTemplateResult.Created(template)
    }

    override suspend fun update(
        id: String,
        name: String,
        body: String,
        kind: String,
        audience: String,
    ): SmsTemplateResult = mutex.withLock {
        validationError(name, body, kind, audience)?.let { return@withLock SmsTemplateResult.Failed(it) }
        val templates = loadTemplates()
        val existing = templates.firstOrNull { it.id == id }
            ?: return@withLock SmsTemplateResult.Failed("Local template was not found.")
        val template = existing.copy(
            name = name.trim(),
            kind = kind,
            audience = audience,
            body = body.trim(),
        )
        val updated = templates.map { if (it.id == id) template else it }
        if (!writeTemplates(updated)) return@withLock SmsTemplateResult.Failed("Cannot save the local template.")
        SmsTemplateResult.Updated(template)
    }

    private fun loadTemplates(): List<SmsTemplate> = runCatching {
        if (!file.baseFile.exists()) return DEFAULT_TEMPLATES
        val array = JSONArray(file.readFully().toString(Charsets.UTF_8))
        buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.toTemplate()?.let(::add)
            }
        }.takeIf(List<SmsTemplate>::isNotEmpty)?.map(::upgradeLegacySystemTemplate) ?: DEFAULT_TEMPLATES
    }.getOrDefault(DEFAULT_TEMPLATES)

    private fun upgradeLegacySystemTemplate(template: SmsTemplate): SmsTemplate {
        if (template.id != "local-booking-new" || template.body != LEGACY_NEW_BOOKING_BODY) return template
        return DEFAULT_TEMPLATES.first { it.id == template.id }
    }

    private fun writeTemplates(templates: List<SmsTemplate>): Boolean {
        val payload = JSONArray().apply { templates.forEach { put(it.toJson()) } }.toString().toByteArray()
        val stream = runCatching(file::startWrite).getOrNull() ?: return false
        return try {
            stream.write(payload)
            file.finishWrite(stream)
            true
        } catch (_: Exception) {
            file.failWrite(stream)
            false
        }
    }

    private fun validationError(name: String, body: String, kind: String, audience: String): String? = when {
        name.isBlank() || name.length > 80 -> "Template name is required."
        body.isBlank() || body.length > 1_000 -> "Template content is required."
        kind !in setOf("MESSAGE", "BOOKING_FORM") -> "Unsupported template type."
        audience !in setOf("NEW", "RETURNING", "ALL") -> "Unsupported template audience."
        kind == "BOOKING_FORM" && !body.contains("{{formUrl}}") -> "Form template must contain {{formUrl}}."
        else -> null
    }

    private companion object {
        const val FILE_NAME = "sms-templates.json"
        val DEFAULT_TEMPLATES = listOf(
            SmsTemplate(
                id = "local-booking-new",
                name = "Formularz — nowy klient",
                kind = "BOOKING_FORM",
                audience = "NEW",
                body = """Dzień dobry {{name}}! Tu mobilny serwis Oponexis.
Termin wizyty: {{visitDate}}, {{visitTime}}.
Prosimy uzupełnić formularz potrzebny do realizacji wizyty oraz wskazać, skąd dowiedzieli się Państwo o Oponexis.
Formularz: {{formUrl}}""",
                system = true,
            ),
            SmsTemplate(
                id = "local-booking-returning",
                name = "Formularz — stały klient",
                kind = "BOOKING_FORM",
                audience = "RETURNING",
                body = """Dzień dobry {{name}}! Tu mobilny serwis Oponexis.
Termin wizyty: {{visitDate}}, {{visitTime}}.
Mamy zapisane dane z poprzedniej wizyty:
{{savedDetails}}

Prosimy otworzyć formularz, sprawdzić dane i wysłać go bez zmian albo poprawić wybrane informacje.
Formularz: {{formUrl}}""",
                system = true,
            ),
            SmsTemplate(
                id = "local-review-request",
                name = "Podziękowanie i prośba o opinię",
                kind = "MESSAGE",
                audience = "ALL",
                body = "Dziękujemy za skorzystanie z usług Oponexis. Jeśli wszystko przebiegło dobrze, będziemy wdzięczni za krótką opinię: https://oponexis.pl",
                system = true,
            ),
        )

        val LEGACY_NEW_BOOKING_BODY = """Dzień dobry! Tu mobilny serwis Oponexis.
Termin wizyty: {{visitDate}}, {{visitTime}}.
Prosimy uzupełnić formularz potrzebny do realizacji wizyty oraz wskazać, skąd dowiedzieli się Państwo o Oponexis.
Formularz: {{formUrl}}"""
    }
}

private fun SmsTemplate.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("kind", kind)
    .put("audience", audience)
    .put("body", body)
    .put("system", system)

private fun JSONObject.toTemplate(): SmsTemplate? {
    val id = optString("id").trim()
    val name = optString("name").trim()
    val kind = optString("kind").trim()
    val audience = optString("audience").trim()
    val body = optString("body").trim()
    if (id.isEmpty() || name.isEmpty() || kind.isEmpty() || audience.isEmpty() || body.isEmpty()) return null
    return SmsTemplate(id, name, kind, audience, body, optBoolean("system", false))
}
