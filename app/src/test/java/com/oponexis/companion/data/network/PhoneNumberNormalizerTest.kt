package com.oponexis.companion.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberNormalizerTest {
    @Test
    fun `normalizes nine digit Polish number like CRM`() {
        assertEquals("+48123456789", PhoneNumberNormalizer.normalize("123 456 789"))
    }

    @Test
    fun `preserves explicit country prefix and removes separators`() {
        assertEquals("+48123456789", PhoneNumberNormalizer.normalize("+48 123-456-789"))
    }

    @Test
    fun `rejects input without digits`() {
        assertNull(PhoneNumberNormalizer.normalize("unknown"))
    }

    @Test
    fun `does not treat non ASCII numerals as CRM digits`() {
        assertNull(PhoneNumberNormalizer.normalize("١٢٣٤٥٦٧٨٩"))
    }
}
