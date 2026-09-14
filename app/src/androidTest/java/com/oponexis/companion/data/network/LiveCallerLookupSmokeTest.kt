package com.oponexis.companion.data.network

import androidx.test.platform.app.InstrumentationRegistry
import com.oponexis.companion.BuildConfig
import com.oponexis.companion.domain.callers.CallerCardStore
import com.oponexis.companion.domain.callers.CallerLookupCoordinator
import com.oponexis.companion.domain.model.CallerCardState
import com.oponexis.companion.domain.network.NetworkRecoveryWaiter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit

class LiveCallerLookupSmokeTest {
    @Test
    fun localConfiguredGatewayReturnsNotFoundForSyntheticNumber() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Run only with -e liveCrm true", arguments.getString("liveCrm") == "true")

        val config = CrmNetworkConfig(BuildConfig.CRM_BASE_URL, BuildConfig.CRM_API_TOKEN)
        assumeTrue("Local CRM configuration is required", config.isConfigured)
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .build()
            .create(OpenXApi::class.java)
        val cardStore = CallerCardStore()
        val coordinator = CallerLookupCoordinator(
            repository = RetrofitCallerLookupRepository(api, config),
            cardStore = cardStore,
            networkRecoveryWaiter = NetworkRecoveryWaiter { false },
        )

        coordinator.lookup(SYNTHETIC_NO_MATCH_NUMBER)

        assertEquals(CallerCardState.NotFound, cardStore.state.value)
    }

    private companion object {
        const val SYNTHETIC_NO_MATCH_NUMBER = "+48000000000"
    }
}
