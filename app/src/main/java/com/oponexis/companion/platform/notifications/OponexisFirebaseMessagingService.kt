package com.oponexis.companion.platform.notifications

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oponexis.companion.platform.sync.MobilePushSyncScheduler
import com.oponexis.companion.platform.sms.DirectSmsScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Firebase Messaging 25 uses onRegistered(FID); Android lint still checks the deprecated token callback.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
class OponexisFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var scheduler: MobilePushSyncScheduler
    @Inject lateinit var notifications: SmsActivityNotificationManager
    @Inject lateinit var directSmsScheduler: DirectSmsScheduler

    override fun onRegistered(installationId: String) {
        scheduler.registerInstallation(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "sms_dispatch_pending") {
            directSmsScheduler.sync()
            return
        }
        if (message.data["type"] != "sms_activity_changed") return
        scheduler.syncActivity(message.data["eventId"])
        val status = message.data["status"] ?: return
        val source = message.data["source"] ?: "PLATFORM"
        notifications.showStatus(status, source, message.data["providerMessageId"])
    }
}
