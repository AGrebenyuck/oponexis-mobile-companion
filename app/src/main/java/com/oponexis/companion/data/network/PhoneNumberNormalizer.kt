package com.oponexis.companion.data.network

internal object PhoneNumberNormalizer {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val hasPlus = trimmed.startsWith('+')
        val digits = trimmed.filter { it in '0'..'9' }
        if (digits.isEmpty()) return null

        return when {
            hasPlus -> "+$digits"
            digits.length == 9 -> "+48$digits"
            else -> "+$digits"
        }
    }
}
