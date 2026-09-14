package com.oponexis.companion

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.oponexis.companion.platform.sync.OutboxRecoveryCoordinator
import com.oponexis.companion.platform.sync.MobilePushSyncScheduler
import com.oponexis.companion.platform.sync.SmsStatusRecoveryCoordinator
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.installations.FirebaseInstallations
import com.oponexis.companion.platform.sms.DirectSmsScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class OponexisApplication : Application() {
    @Inject lateinit var outboxRecovery: OutboxRecoveryCoordinator
    @Inject lateinit var mobilePushSync: MobilePushSyncScheduler
    @Inject lateinit var smsStatusRecovery: SmsStatusRecoveryCoordinator
    @Inject lateinit var directSmsScheduler: DirectSmsScheduler

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            outboxRecovery.recover()
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            smsStatusRecovery.recover()
        }
        FirebaseMessaging.getInstance().register()
        FirebaseInstallations.getInstance().id.addOnSuccessListener { installationId ->
            mobilePushSync.registerInstallation(installationId)
        }
        mobilePushSync.syncActivity()
        directSmsScheduler.sync()
    }
}
