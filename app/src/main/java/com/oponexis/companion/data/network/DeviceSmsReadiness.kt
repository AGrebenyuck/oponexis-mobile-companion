package com.oponexis.companion.data.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface DeviceSmsReadiness {
    fun unavailableReason(): String?
}

@Singleton
class AndroidDeviceSmsReadiness @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceSmsReadiness {
    override fun unavailableReason(): String? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return "companion_sms_no_telephony"
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return "companion_sms_permission_missing"
        }
        val telephony = context.getSystemService(TelephonyManager::class.java)
        if (telephony.simState != TelephonyManager.SIM_STATE_READY) {
            return "companion_sms_sim_not_ready"
        }
        return null
    }
}
