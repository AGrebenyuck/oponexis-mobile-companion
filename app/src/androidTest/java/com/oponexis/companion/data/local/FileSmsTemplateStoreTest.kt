package com.oponexis.companion.data.local

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oponexis.companion.domain.model.SmsTemplateResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileSmsTemplateStoreTest {
    @Test
    fun providesDefaultsAndPersistsPhoneLocalEdits() = runTest {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testFilesDir = targetContext.filesDir.resolve("template-store-test").apply { mkdirs() }
        val context = object : ContextWrapper(targetContext) {
            override fun getFilesDir() = testFilesDir
            override fun getApplicationContext(): Context = this
        }
        context.filesDir.resolve("sms-templates.json").delete()
        val store = FileSmsTemplateStore(context)

        val initial = store.templates("NEW") as SmsTemplateResult.Loaded
        assertEquals(2, initial.templates.size)
        assertTrue(initial.templates.first { it.id == "local-booking-new" }.body.contains("{{name}}"))

        val created = store.create("Lokalny", "Oddzwonimy jutro.", "MESSAGE", "NEW")
            as SmsTemplateResult.Created
        val updated = store.update(
            id = created.template.id,
            name = created.template.name,
            body = "Oddzwonimy dzisiaj.",
            kind = created.template.kind,
            audience = created.template.audience,
        )
        assertTrue(updated is SmsTemplateResult.Updated)

        val persisted = FileSmsTemplateStore(context).templates("NEW") as SmsTemplateResult.Loaded
        assertEquals("Oddzwonimy dzisiaj.", persisted.templates.first { it.id == created.template.id }.body)
        context.filesDir.resolve("sms-templates.json").delete()
        testFilesDir.delete()
    }
}
