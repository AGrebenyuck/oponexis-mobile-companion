package com.oponexis.companion.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.oponexis.companion.domain.network.NetworkRecoveryWaiter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class AndroidNetworkRecoveryWaiter @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkRecoveryWaiter {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    override suspend fun awaitRecovery(): Boolean = try {
        withTimeoutOrNull(MAX_WAIT_MILLIS) {
            callbackFlow {
                fun reportIfUsable(network: Network) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    if (capabilities?.isUsableForCrm() == true) {
                        trySend(Unit)
                    }
                }

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = reportIfUsable(network)

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        if (networkCapabilities.isUsableForCrm()) {
                            trySend(Unit)
                        }
                    }
                }

                connectivityManager.registerDefaultNetworkCallback(callback)
                connectivityManager.activeNetwork?.let(::reportIfUsable)
                awaitClose {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
            }.first()
            true
        } ?: false
    } catch (_: SecurityException) {
        false
    }

    private fun NetworkCapabilities.isUsableForCrm(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)

    private companion object {
        const val MAX_WAIT_MILLIS = 5_000L
    }
}
