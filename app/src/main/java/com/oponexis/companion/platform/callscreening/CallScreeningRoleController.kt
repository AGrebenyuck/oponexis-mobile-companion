package com.oponexis.companion.platform.callscreening

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class CallScreeningRoleStatus(
    val available: Boolean,
    val held: Boolean,
)

@Singleton
class CallScreeningRoleController @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val roleManager = context.getSystemService(RoleManager::class.java)

    fun status(): CallScreeningRoleStatus {
        val available = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
        return CallScreeningRoleStatus(
            available = available,
            held = available && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING),
        )
    }

    fun createRequestIntent(): Intent? {
        val currentStatus = status()
        return if (currentStatus.available && !currentStatus.held) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        } else {
            null
        }
    }
}
