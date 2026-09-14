package com.oponexis.companion.data.network

data class CrmNetworkConfig(
    val baseUrl: String,
    val apiToken: String,
) {
    val isConfigured: Boolean
        get() = apiToken.isNotBlank() && baseUrl != INVALID_BASE_URL

    private companion object {
        const val INVALID_BASE_URL = "https://invalid.local/"
    }
}
