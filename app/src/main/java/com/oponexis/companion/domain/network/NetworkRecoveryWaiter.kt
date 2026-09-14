package com.oponexis.companion.domain.network

fun interface NetworkRecoveryWaiter {
    suspend fun awaitRecovery(): Boolean
}
