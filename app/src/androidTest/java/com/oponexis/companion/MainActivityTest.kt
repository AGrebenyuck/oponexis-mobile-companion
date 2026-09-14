package com.oponexis.companion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun applicationId_isCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.oponexis.companion.dev", context.packageName)
    }
}
